package com.bertramlabs.plugins.hcl4j

import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.*
import com.bertramlabs.plugins.hcl4j.symbols.Symbol
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

/**
 * @author David Estes
 */
class HCLParserSpec extends Specification {

	void "should generate Map from hcl"() {
		given:

		def hcl = '''
variables {
test = "value"
}

service "myservice" {
  description = "test"
  info {
    name = "my name"
    maxMemory = 1024
    priority = 0.1
    enabled = true
    tags = ["love",1,2,3,["nested","2"]]
  }
}

resource "lbs" {
	name = "lb1"
}

resource "lbs" {
	name = "lb2"
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variables') == true
		results.containsKey('service') == true
		results.service.containsKey("myservice")
		results.service.myservice.description == "test"
		results.service.myservice.info.name == "my name"
		results.service.myservice.info.enabled == true
		results.service.myservice.info.maxMemory == 1024
		results.service.myservice.info.tags.size() == 5
		results.service.myservice.info.tags[4].size() == 2
		results.resource.lbs.size() == 2
	}


	void "it should handle a vmware tf file"() {
		given:
		def hcl = '''
variable "cloudPassword" {
}

provider "vsphere" {
  user           = "administrator@vmware.bertramlabs.com"
  password       = "${var.cloudPassword}"
  vsphere_server = "10.30.21.180"
  version = "~> 1.3.0"
  # if you have a self-signed cert
  allow_unverified_ssl = true
}

data "vsphere_datacenter" "dc" {
  name = "labs-denver"
}

data "vsphere_datastore" "datastore" {
  name = "labs-qa-qnap-240"
  datacenter_id = "${data.vsphere_datacenter.dc.id}"
}

data "vsphere_resource_pool" "pool" {
  name = "labs-den-qa-cluster/Resources"
  datacenter_id = "${data.vsphere_datacenter.dc.id}"
}

data "vsphere_network" "network" {
  name = "VM Network"
  datacenter_id = "${data.vsphere_datacenter.dc.id}"
}

data "vsphere_virtual_machine" "template" {
  name = "Morpheus Ubuntu 16.04.3 v1"
  datacenter_id = "${data.vsphere_datacenter.dc.id}"
}

resource "vsphere_virtual_machine" "tm-terraform-1" {
  name = "tm-terraform-1"
  resource_pool_id = "${data.vsphere_resource_pool.pool.id}"
  datastore_id = "${data.vsphere_datastore.datastore.id}"
  num_cpus = 2
  memory = 1024
  guest_id = "ubuntu64Guest"

  network_interface {
    network_id = "${data.vsphere_network.network.id}"
  }

  disk {
    label = "disk0"
    thin_provisioned = true
    size  = 20
  }

  clone {
    template_uuid = "${data.vsphere_virtual_machine.template.id}"
  }

  connection {
    type = "ssh"
    user = "cloud-user"
    password = "m0rp#3us!"
  }
}

resource "vsphere_virtual_machine" "tm-terraform-2" {
  name = "tm-terraform-2"
  resource_pool_id = "${data.vsphere_resource_pool.pool.id}"
  datastore_id = "${data.vsphere_datastore.datastore.id}"
  num_cpus = 1
  memory = 512
  guest_id = "ubuntu64Guest"

  network_interface {
    network_id = "${data.vsphere_network.network.id}"
  }

  disk {
    label = "disk0"
    thin_provisioned = true
    size  = 20
  }

  clone {
    template_uuid = "${data.vsphere_virtual_machine.template.id}"
  }

  connection {
    type = "ssh"
    user = "cloud-user"
    password = "m0rp#3us!"
  }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true
		results.containsKey('resource') == true
	}



	void "should handle Map parsing"() {
		given:

		def hcl = '''
variable {
test = {"list": [1,2,3,[4,5,6]], name: "David Estes", info: { firstName: "David", lastName: "Estes", aliases: []}}
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true
		results.variable.test instanceof Map
		results.variable.test.list.size() == 4
	}

	void "should handle block attribute identifiers" () {
		given:
		def hcl = '''
resource xxx "images" {
  default = "empty.jpg"
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		then:
		results.resource.xxx.images.default == "empty.jpg"
	}


	void "should handle Map parsing"() {
		given:

		def hcl = '''
variable {
test = {"list": [1,2,3,[4,5,6]], name: "David Estes", info: { firstName: "David", lastName: "Estes", aliases: []}}
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true
		results.variable.test instanceof Map
		results.variable.test.list.size() == 4
	}


	void "should handle multiple locals{} blocks for context"() {
		given:
		def hcl = '''
locals {
  domains = ["a.example.com", "b.example.com"]
}
locals {
 c = "Hello there person"
}

resource "test_instance" "test" {
  name = local.c
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.resource.test_instance.test.name == "Hello there person"
	}



	void "should handle complex type parsing"() {
		given:
		def hcl = '''
variable "cluster_enabled_log_types" {
    description = "A list of the desired control plane logs to enable. For more information, see Amazon EKS Control Plane Logging documentation (https://docs.aws.amazon.com/eks/latest/userguide/control-plane-logs.html)"
    type        = list(string)
    myany       = list(any)
    default     = ["audit", "api", "authenticator"]
}

variable "cluster_encryption_config" {
    description = "Configuration block with encryption configuration for the cluster"
    type = list(object({
        provider_key_arn = string
        resources        = list(string)
    }))
    default = []
}

variable "tags" {
    description = "A map of tags to add to all resources"
    type        = map(string)
    default     = {}
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		then:
		results.variable.tags.type instanceof PrimitiveType
	}

	void "should handle primitive type parsing"() {
		given:
		def hcl = '''
variable {
	typeString = string
	typeNumber = number
	typeBoolean = boolean
	typeMap = map(number)
	typeList = list
	typeListString = list(string)
	typeSetNumber = set(number)
	typeMapListString = map(list(string))
}
		'''
				HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		then:
		results.containsKey('variable') == true
		results.variable.typeString instanceof StringPrimitiveType
		results.variable.typeList instanceof ListPrimitiveType
		results.variable.typeListString instanceof ListPrimitiveType
		results.variable.typeListString.getChildren().get(0) instanceof StringPrimitiveType
		results.variable.typeSetNumber instanceof SetPrimitiveType
		results.variable.typeMap instanceof MapPrimitiveType
		results.variable.typeMapListString instanceof MapPrimitiveType
		results.variable.typeMapListString.getChildren().get(0) instanceof ListPrimitiveType
		results.variable.typeMapListString.getChildren().get(0).getChildren().get(0) instanceof StringPrimitiveType
	}


	void "should handle multiple list types"() {
		given:
		def hcl = '''
variable "container_subnet_ids" {
type = list(string)
default = ["subnet-72b9162b"]
}



variable "yellow_subnet_ids" {
type = list(string)
default = ["subnet-72b9162b"]
}
'''
				HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		then:
		results.containsKey('variable') == true
		results.variable.yellow_subnet_ids.type  instanceof ListPrimitiveType
		results.variable.yellow_subnet_ids['default'].size() == 1
	}


	void "should handle method in interpolation syntax"() {
		given:
		def hcl = '''
		resource "aws_instance" "test" {
			tags {
			Author = "Effectual Terraform script"
			Date ="${timestamp()}"
			}			
		}
'''
 		HCLParser parser = new HCLParser();
 		when:
 		def results = parser.parse(hcl)
 		then:
 		results.resource.aws_instance.test.tags.Date != null
 		
	}




	void "it should handle jsonencode nested multiline arguments"() {
		given:
		def hcl = '''
resource "aws_ecs_task_definition" "service" {
  family = "service"
  container_definitions = jsonencode([
    {
      name      = "first"
      image     = "service-first"
      cpu       = 10
      memory    = 512
      essential = true
      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
        }
      ]
    },
    {
      name      = "second"
      image     = "service-second"
      cpu       = 10
      memory    = 256
      essential = true
      portMappings = [
        {
          containerPort = 443
          hostPort      = 443
        }
      ]
    }
  ])

  volume {
    name      = "service-storage"
    host_path = "/ecs/service-storage"
  }

  placement_constraints {
    type       = "memberOf"
    expression = "attribute:ecs.availability-zone in [us-west-2a, us-west-2b]"
  }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		new JsonSlurper().parseText(results.resource.aws_ecs_task_definition.service?.container_definitions)[0].name == "first"
	}


	void "it should decode json"() {
		given:
		def hcl = '''
  container_definitions = jsondecode(jsonencode([
    {
      name      = "first"
      image     = "service-first"
      cpu       = 10
      memory    = 512
      essential = true
      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
        }
      ]
    },
    {
      name      = "second"
      image     = "service-second"
      cpu       = 10
      memory    = 256
      essential = true
      portMappings = [
        {
          containerPort = 443
          hostPort      = 443
        }
      ]
    }
  ]))
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.container_definitions[0].name == "first"
	}

	void "should handle interpolation syntax"() {
		given:

		def hcl = '''
variable {
firstName = "David"
lastName = "Estes"
fullName = "${var.firstName} ${var["lastName"]}"
escapedInterpolation = "$${var.firstName}"
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true
		results.variable.fullName == 'David Estes'
		results.variable.escapedInterpolation == '$${var.firstName}'
	}


	void "should handle multiline string"() {
		given:

		def hcl = '''
variable {
description = <<EOL
This is a cool String
I love multiple lines
Don't you?
EOL
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true
		results.variable.description == '''This is a cool String
I love multiple lines
Don't you?
'''
	}

	void "should handle stripped tabs multiline string"() {
		def hcl = '''
		user_data = <<-EOF
		  #!/bin/bash
		  echo "Hello world" > index.html
		  nohup busybox httpd -f -p 8080 &
		  EOF
		'''
		 HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('user_data') == true
	}


	void "it should handle single line blocks that are empty"() {
		given:

		def hcl = '''
variable {}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.containsKey('variable') == true

	}

	void "it should handle root level attributes"() {
		given:

		def hcl = '''
foo = "Hello there"
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.foo == "Hello there"
	}


	void "it should handle values of variables being a block type"() {
		given:

		def hcl = '''
variable "images" {
	type = "map"
	default = {
		us-east-1 = "image-1234"
		us-west-1 = "image-4567"
	}
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.variable.images.default['us-east-1'] == "image-1234"
	}

	void "it should handle multiline interpolated strings" () {
		given:
		def hcl = '''
variable "instance_image_ocid" {
type = "map"
 default {
   us-phoenix-1 = "ocid1.image.oc1.phx.aaaaaaaasc56hnpnx7swoyd2fw5gyvbn3kcdmqc2guiiuvnztl2erth62xnq"
   us-ashburn-1 = "ocid1.image.oc1.iad.aaaaaaaaxrqeombwty6jyqgk3fraczdd63bv66xgfsqka4ktr7c57awr3p5a"
   eu-frankfurt-1 = "ocid1.image.oc1.eu-frankfurt-1.aaaaaaaayxmzu6n5hsntq4wlffpb4h6qh6z3uskpbm5v3v4egqlqvwicfbyq"
 }
}

resource "random_integer" "instance_ad_shift" {
 max = 5
 min = 0
}

resource "oci_core_instance" "compute_instances" {

 count = "${var.env["ddr.instance.count"]}"

 availability_domain = "${
 element(
   split("|",
         element(
           split(",",
                 var.cloud["oci.network.vcn.subnets"]
           ),
         ( random_integer.instance_ad_shift.result + count.index ) % length(
                       split(",",
                             var.cloud["oci.network.vcn.subnets"]
                       )
                      )
     )
   ),
   0
 )
 }"
 compartment_id = "${var.cloud["oci.compartment.ocid"]}"
 display_name = "${var.env["name.short"]}-ddr-${count.index + 1 }"
 image = "${var.instance_image_ocid[var.cloud["oci.region.name"]]}"
 shape = "${var.env["ddr.instance.shape"]}"

 create_vnic_details {
   subnet_id = "${
   element(
     split("|",
         element(
           split(",",
                 var.cloud["oci.network.vcn.subnets"]
           ),
         ( random_integer.instance_ad_shift.result + count.index ) % length(
                       split(",",
                             var.cloud["oci.network.vcn.subnets"]
                       )
                      )
       )
     ),
     1
 )
 }"
   display_name = "${var.env["name.short"]}-ddr-${count.index + 1}"
   assign_public_ip = false
   hostname_label = "${var.env["name.short"]}-ddr-${count.index + 1}"
 }

 metadata {
   ssh_authorized_keys = "${var.cloud["oci.host.ssh.key.public"]}"
   user_data = "${base64encode(file("${path.module}/files/bootstrap.sh"))}"
 }

 timeouts {
   create = "60m"
 }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.variable.instance_image_ocid.type == 'map'
	}


	void "it should handle dollar sign in the password field"() {
				given:

		def hcl = '''
variable "credentials" {
	password = "Pa$sword"
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.variable.credentials.password == 'Pa$sword'
	}

	void "it should handle quoted property names"() {
					given:

		def hcl = '''
# https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/concepts.platforms.html
 available_stack_regex = {
   "docker"        = "^64bit Amazon Linux (.*) Docker 17.09.1-ce$"
   "docker-esc"    = "^64bit Amazon Linux (.*) Multi-container Docker 17.09.1-ce (Generic)$"
   "dotnet"        = "^64bit Windows Server 2016 (.*) IIS 10.0$"
   "dotnet-wsc"    = "^64bit Windows Server Core 2016 (.*) IIS 10.0$"
   "go1.9"         = "^64bit Amazon Linux (.*) Go 1.9$"
   "java8"         = "^64bit Amazon Linux (.*) Java 8$"
   "java8-tomcat8" = "^64bit Amazon Linux (.*) Tomcat 8 Java 8$"
   "nodejs"        = "^64bit Amazon Linux (.*) Node.js$"
   "php5.6"        = "^64bit Amazon Linux (.*) PHP 5.6$"
   "php7.0"        = "^64bit Amazon Linux (.*) PHP 7.0$"
   "php7.1"        = "^64bit Amazon Linux (.*) PHP 7.1$"
   "python3.6"     = "^64bit Amazon Linux (.*) Python 3.6$"
   "python2.7"     = "^64bit Amazon Linux (.*) Python 2.7$"
 }
'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.available_stack_regex['python2.7'] == '^64bit Amazon Linux (.*) Python 2.7$'
	}


  void "it should handle map objects"() {
  	given:
  	def hcl = '''
my_object = {
  object_elem_a: 1,
  object_elem_b: 2
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.my_object.object_elem_a == 1
  }

	void "it should handle closures inside an array"() {
		given:
		def hcl = '''
array_set = [{
  blah = "test"
},
{blah = "test2"}
]
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.array_set?.size() == 2
		results.array_set[1].blah == "test2"
	}

	void "it should handle end of line comments with hyphens"() {
		given:
		def hcl = '''
terraform {
  backend "s3" {
    bucket = "my-configuration"     # in my-main account
    key = "my-terraform-state"
    region = "us-west-1"
  }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		then:
		results.terraform.backend.s3.bucket == "my-configuration"
	}


	void "it should handle comments inside an array"() {
		given:
		def hcl = '''
array_set = [ "test", #comment goes here
"test2" #another comment
]
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.array_set?.size() == 2
	}

	void "it should handle hyphens in a comment"() {
		given:
		def hcl = '''
terraform {
  backend "s3" {
    bucket = "my-configuration"     # in my-main account
    key = "my-terraform-state"
    region = "us-west-1"
  }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		then:
		results.terraform.backend.s3.bucket == "my-configuration"
	}



	void "it should handle variable references"() {
		given:
		def hcl = '''
		array_set = myvariable.user
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
//		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.array_set instanceof Variable
		results.array_set.getChildren().size() > 0
		results.array_set.name == 'myvariable.user'
	}

	void "substr should function correctly"() {
		given:
		def hcl = '''
test1 = substr("hello world", 1, 4)
test2 = substr("hello world", -5, -1)
test3 = substr("hello world", 6, 10)
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		then:
		results.test1 == "ello"
		results.test2 == "world"
		results.test3 == "world"
	}


	void "it should handle conditional expressions"() {
		given:
		def hcl = '''
worldVar = "world"
testConditional = 1 < 2 ? "yes" : "no"
testConditional2 = 3 < 2 ? "yes" : "no"
testConditional3 = "hello" == "hello" ? "yes" : "no"
testConditional4 = "hello" != "hello" ? "yes" : "no"
testConditional5 = 3 < 2 ? "yes" : (4 > 2 ? "maybe" : "maybe not")
testConditional6 = worldVar == "world" ? "yes" : "no"
testConditional7 = worldVar == "world" && 3 > 2 ? "yes" : "no"
testConditional8 = worldVar == "world" && 3 < 2 ? "yes" : "no"
testConditional9 = worldVar == "world" || 3 < 2 ? "yes" : "no"
locals {
	size = "${var.instance_size == "m4.xlarge" ? "m4.xlarge" : ( var.instance_size == "m4.large" ? "m4.large" : ( var.instance_size == "t2.medium" ? "t2.medium" : "t2.micro" )) }"
}



'''
		HCLParser parser = new HCLParser();
		parser.setVariable("instance_size","blah")

		when:
		def results = parser.parse(hcl)
		then:
		results.testConditional == "yes"
		results.testConditional2 == "no"
		results.testConditional3 == "yes"
		results.testConditional4 == "no"
		results.testConditional5 == "maybe"
		results.testConditional6 == "yes"
		results.testConditional7 == "yes"
		results.testConditional8 == "no"
		results.testConditional9 == "yes"
		results.locals.size == "t2.micro"

	}


	void "format should function correctly"() {
		given:
		def hcl = '''
test1 = format("%s %s", "hello", "world")
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		then:
		results.test1 == "hello world"
	}

	void "it should runtime evaluate variables"() {
		given:

		def tfvars = '''
test_var = "testvar1"
'''
		def hcl = '''
        variable "test_var" {
          default = "blah"
        }
        variable "code_var" {
          default="local_code"
        }

		locals {
		  local_var = "localvar1"
		  my_tags = ["A","B","C"]
		  local_code = "localcode1"
		  start = 1
		  end = 3
		  maybe = true
		}
		
		resource "test" "me" {
		 name = upper(local.local_var)
		 description = var.test_var
		 should_not_exist = var.null_var
		 code = local[var.code_var]
		 label = lower(local.my_tags[1])
		 labelContains = contains(local.my_tags,"C")
		 math = local.start + local.end
		 operations = 2 + 4 / 2
		 operationsOrder = 2 + (4 / 2)
		 perhaps = ! local.maybe
		}
'''
		HCLParser parser = new HCLParser();
		when:
		parser.parseVars(tfvars,false);
		def results = parser.parse(hcl)
		then:
		results.resource.test.me.name == "LOCALVAR1"
		results.resource.test.me.description == "testvar1"
		results.resource.test.me.code == "localcode1"
		results.resource.test.me.label == "b"
		results.resource.test.me.labelContains == true
		results.resource.test.me.math == 4
		results.resource.test.me.should_not_exist == null
		results.resource.test.me.operations == 3
		results.resource.test.me.operationsOrder == 4
		results.resource.test.me.perhaps == false

	}

	void "it should handle lack of spaces between operators"() {
		given:
				def tfvars = '''
storage_type = "io1"
iops=1000
max_allocated_storage=1000
'''
		def hcl = '''
locals { var_iops = { value = var.storage_type == "io1" ? max(var.iops, var.max_allocated_storage*0.5) : null}}
'''
		HCLParser parser = new HCLParser();
		when:
		parser.parseVars(tfvars,false);
		def results = parser.parse(hcl)
		then:
		results.locals.var_iops.value == 1000
	}



//	void "it should ignore complex for loops for now"() {
//		given:
//		def hcl = '''
//output "instance_public_ip_addresses" {
//  value = {
//    for instance in aws_instance.example:
//    instance.id => instance.public
//    if instance.associate_public_ip_address
//  }
//}
//'''
//		HCLParser parser = new HCLParser();
//		when:
//		def results = parser.parse(hcl)
//		then:
//		results.output != null
//	}


	void "it should handle closing block on same line as value hcl2"() {
		given:
		def hcl = '''
		provider "vsphere" {
  user           = var.vsphereUsername
  password       = var.vspherePassword
  vsphere_server = var.vsphereUrl
  version = "~> 1.11.0"
  allow_unverified_ssl = true
}

data "vsphere_datacenter" "dc" {
  name = "wheeler"
}

data "vsphere_datastore" "datastore" {
  name = "wheeler-vsan"
  datacenter_id = data.vsphere_datacenter.dc.id
}

data "vsphere_resource_pool" "pool" {
  name = "terraform"
  datacenter_id = "${data.vsphere_datacenter.dc.id}"
}

data "vsphere_network" "network" {
  name = "wheeler-vms"
  datacenter_id = data.vsphere_datacenter.dc.id
}

data "vsphere_virtual_machine" "template" {
  name = "Morpheus Ubuntu 18.04.2 v1"
  datacenter_id = data.vsphere_datacenter.dc.id
}

# Create (and display) an SSH key
resource "tls_private_key" "bw-key-1" {
  algorithm = "RSA"
  rsa_bits = 4096
}

output "tls_private_key" { value = tls_private_key.bw-key-1.private_key_pem }

#create a vm
resource "vsphere_virtual_machine" "vm-1" {
  name = "terraform-test-1"
  resource_pool_id = data.vsphere_resource_pool.pool.id
  datastore_id = data.vsphere_datastore.datastore.id
  num_cpus = 1
  memory = 1024
  guest_id = "ubuntu64Guest"

  network_interface {
    network_id = data.vsphere_network.network.id
  }

  disk {
    label = "disk0"
    thin_provisioned = true
    size  = 20
  }

  clone {
    template_uuid = data.vsphere_virtual_machine.template.id
  }

  connection {
    type = "ssh"
    user = "cloud-user"
    private_key = tls_private_key.bw-key-1
    password = "password"
  }

}

'''
	HCLParser parser = new HCLParser();
	when:
	def results = parser.parse(hcl)
	println results
	then:
	results.resource["vsphere_virtual_machine"]["vm-1"] != null
	}


	void "it should handle multiline delimiters"() {
		given:
		def hcl = '''
#################################
##  Server Specs         ##
#################################
resource "aws_launch_configuration" "web" {
	//name = "${aws_vpc.main.id}-webLC"
	name_prefix = "{aws_vpc.main.id}-webLC"
	image_id = "ami-dbc8e3be"
	instance_type = "t2.micro"
	security_groups = ["${aws_security_group.main.id}"]

	user_data = <<-EOF
              #!/bin/bash
              echo "Hello, World" > index.html
              nohup busybox httpd -f -p 8080 &
              EOF
  
	lifecycle {
		create_before_destroy = true
	}
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		println("results: ${results}")

		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.resource["aws_launch_configuration"]["web"]?.user_data != null
	}

	void "it should handle nested for tuples"() {
		given:
		def hcl = '''
locals {
swagger_path_method_parameters = [for my_value in local.swagger_path_methods_split: 
      [for method_name, method_value in local.json_data["paths"][my_value[0]][my_value[1]]: format("%s:::%s:::%s", my_value[0], my_value[1], method_name)
      ]
    ]
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		println results
		then:
		results.locals["swagger_path_method_parameters"] != null

	}

	void "it should handle sample tf from wu"() {
		given:
			def hcl = '''
#
#
# TODO: 2/10/2022: Modify code to handle all "headers" in resource.method.params
  # modify the filter in swagger_path_method_parameter_headers -> only filter on header_dict["in"]
  # modoify the static list of required_value{}
  # Add the final list of scoped optional values (will be a lot +++) to data_set_api_gateway_configuration_data_request_parameters

locals {
  #
  # OnePoint Utility Server
  opm_utility_server = "10.39.5.165:80"
  #
  # HARDCODED
  cloud_name = lower("wureach-np")
  swagger_s3_bucket = "wanv-san-0-poc-s3-poc-9003"
  #
  # Morpheus Cloud Profile
  authorizor_aws_region =  {"name":var.opm_morphues_region}
  authorizor_aws_caller_identity = {"account_id": var.account_number}
  deploment_stage_logs_region = {"name":var.opm_morphues_region}
  deploment_stage_logs_identity = {"account_id": var.account_number}
  #
  # Morpheus Inputs
  s3_folder = replace(lower(local.AppName), "/\\\\s+/", "")
  swagger_file_name =  "<%=customOptions.sidecarListOfS3BucketFolder%>"
  wu_aws_api_gateway_rest_api = "<%=instance.name%>"
  wu_aws_api_gateway_rest_api_description = "<%=customOptions.apiGatewayDescription%>"
  mule_asset_id = "<%=customOptions.mule_asset_id%>"
  mule_group_id = "<%=customOptions.mule_group_id%>"
  swagger_file_version = "<%=customOptions.ot_swagger_file_version%>"
  authorizer_lambda_function = "<%=customOptions.sidecarListOfLambdas%>"
  authorizer_uri = "arn:aws:apigateway:${local.authorizor_aws_region.name}:lambda:path/2015-03-31/functions/arn:aws:lambda:${local.authorizor_aws_region.name}:${local.authorizor_aws_caller_identity.account_id}:function:${local.authorizer_lambda_function}/invocations"
  deployment_stage_name = "dev"
  deployment_stage_var_port = 8201
  deployment_stage_var_hostname = local.target_endpoint_host
  deployment_stage_var_4xxCodeName = "<%=customOptions.deployment_stage_var_4xxCodeName%>"
  deployment_stage_var_5xxCodeName = "<%=customOptions.deployment_stage_var_5xxCodeName%>"
  deployment_stage_setting_throttle_rate = tonumber("<%=customOptions.deployment_stage_setting_throttle_rate%>")
  deployment_stage_setting_throttle_burst = tonumber("<%=customOptions.deployment_stage_setting_throttle_burst%>")
  vpc_endpoint = var.vpc_endpoint == "null" ? "<%=customOptions.sidecarListOfVPCEndpointsApiGateway%>" : var.vpc_endpoint
  integration_aws_api_gateway_vpc_link = "<%=customOptions.sidecarListOfVPCLink%>"
  target_endpoint_host = "<%=customOptions.sidecarListOfTargetEndpointHosts%>"
  gateway_response_type = "<%=customOptions.ag_gateway_response_type%>"
  
  #
  # Terraform Native for this file
  json_file = data.http.opm_utility_server.body
  json_data = jsondecode(local.json_file)
  
  #
  # This block of code, creates the STATIC param/attribute values for api_gateway_integration resource
  swagger_paths = [for index, value in keys(local.json_data["paths"]): value]
  swagger_path_methods = [for index, value in local.swagger_paths: [for method_name, method_value in local.json_data["paths"][value]: format("%s:::%s", value, method_name)]]
  swagger_path_methods_flat = flatten([for my_index, my_list in local.swagger_path_methods: my_list])
  swagger_path_methods_split = [for my_value in local.swagger_path_methods_flat: split(":::", my_value)]
  

  #
  # This block of code  will determine which resource_methods should have the extra REQUEST PARAMS
  swagger_path_method_parameters = [for my_value in local.swagger_path_methods_split: 
      [for method_name, method_value in local.json_data["paths"][my_value[0]][my_value[1]]: format("%s:::%s:::%s", my_value[0], my_value[1], method_name)
      if method_name == "parameters"
      ]
    ]
  swagger_path_method_parameters_flat = flatten([for my_index, my_list in local.swagger_path_method_parameters: my_list])
  swagger_path_method_parameters_split = [for my_value in local.swagger_path_method_parameters_flat: split(":::", my_value)]
  swagger_path_method_parameter_headers = [for my_value in local.swagger_path_method_parameters_split: 
      [for header_dict in local.json_data["paths"][my_value[0]][my_value[1]][my_value[2]]: 
        format("%s:::%s:::%s:::%s", my_value[0], my_value[1], my_value[2], header_dict["name"])
      if 
      (["x-wu-externalRefId", "x-api-key", "x-wu-utkn"], header_dict["name"])
      ]
    ]
  swagger_path_method_parameter_headers_flat = flatten([for my_index, my_list in local.swagger_path_method_parameter_headers: my_list])
  swagger_path_method_parameter_headers_split = [for my_value in local.swagger_path_method_parameter_headers_flat: split(":::", my_value)]
    
  #
  #  # Create the aws_api_gateway_integration Request Parameters
  required_value = {
      "integration.request.header.x-wu-correlationId" = "context.requestId",
      "integration.request.header.x-wu-tenantId" = "context.tenantId",
      "integration.request.header.x-wu-authPrincipal" = "context.authorizer.cid",
      "integration.request.header.x-wu-tokenClm" = "context.authorizer.clm",
      "integration.request.header.x-wu-apiKey" = "method.request.header.x-api-key",
      "integration.request.header.x-wu-externalRefId" = "method.request.header.x-wu-externalRefId",
      "integration.request.header.x-api-key" = "method.request.header.x-api-key",
      "integration.request.header.x-wu-utkn" = "method.request.header.x-wu-utkn"
  }
  data_set_api_gateway_configuration_data_request_parameters = {for resource_method in local.swagger_path_methods_flat: resource_method => 
    {"request_params" = local.required_value}
  }
  data_set_api_gateway_configuration_data_from_data_sources = flatten([ for resource_method in local.swagger_path_methods_flat:
    [for data_resource_item in data.aws_api_gateway_resource.parent: 
      {"${resource_method}" = {"resource_id" = data_resource_item["id"],
        "rest_api_id" = aws_api_gateway_rest_api.parent.id,
        "connection_id" = local.integration_aws_api_gateway_vpc_link,
        "uri" = "https://$${stageVariables.hostName}:$${stageVariables.portNumber}${data_resource_item.path}"
        }
      }
    if split(":::",resource_method)[0] == data_resource_item.path
    ]
    ]
  )
  data_set_api_gateway_configuration_data_static = {for my_value in local.swagger_path_methods_split: format("%s:::%s", my_value[0], my_value[1]) => 
    { "path" = my_value[0],
      "type" = "HTTP_PROXY",
      "method" =  upper(my_value[1]),
      "passthrough" = "WHEN_NO_TEMPLATES", 
      "connection" = "VPC_LINK", 
      "timeout" = 29000}
  }
  final_data_model_for_api_integration = {for idx,resource_method in local.swagger_path_methods_flat: 
    resource_method => merge(
      local.data_set_api_gateway_configuration_data_static[resource_method],
      local.data_set_api_gateway_configuration_data_request_parameters[resource_method],
      local.data_set_api_gateway_configuration_data_from_data_sources[idx][resource_method]
    )
    if lookup(local.data_set_api_gateway_configuration_data_static, resource_method, "None") != "None"  
  }
 
}
##########################################
# Modified Swagger Data
##########################################
data "http" "opm_utility_server" {
  url = "http://${local.opm_utility_server}/create/?cloud_name=${local.cloud_name}&bucket=${local.swagger_s3_bucket}&morpheus_instance_name=${local.wu_aws_api_gateway_rest_api}&application_folder_name=${local.s3_folder}&authorizer_uri=${local.authorizer_uri}&asset_id=${local.mule_asset_id}&group_id=${local.mule_group_id}&version_num=${local.swagger_file_version}&gateway_response_type=${local.gateway_response_type}"
  # Optional request headers
  request_headers = {
    Accept = "application/json"
    Authorization = local.bearer_token_opm_utility
  }
}

##########################################
# RestApi
##########################################
resource "aws_api_gateway_rest_api" "parent" {
  name        = local.wu_aws_api_gateway_rest_api
  # commenting out since we removed from UI, this has introduced a bug where all API are having NULL as description
  # by commenting out the body argument will use the description from the Swagger file if there is one in place
  # description = local.wu_aws_api_gateway_rest_api_description
  body = data.http.opm_utility_server.body
  policy      = <<EOF
  {
   "Version": "2012-10-17",
   "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
            "Service": ["apigateway.amazonaws.com","lambda.amazonaws.com"]
        },
        "Action": "sts:AssumeRole"
      }
    ]
    }
    EOF
  endpoint_configuration {
    types = ["PRIVATE"]
    vpc_endpoint_ids = [local.vpc_endpoint]
  }
  tags = {
    Name = local.wu_aws_api_gateway_rest_api
  }
}



###########################################
## RestApi Top Level Policy
###########################################
resource "aws_api_gateway_rest_api_policy" "parent" {
  rest_api_id = aws_api_gateway_rest_api.parent.id

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": "execute-api:Invoke",
      "Resource": "${aws_api_gateway_rest_api.parent.execution_arn}",
      "Condition": {
        "StringEquals": {
          "aws:SourceVpce": "${local.vpc_endpoint}"
        }
      }
    }
  ]
}
EOF
}

##########################################
# API Authorizer
##########################################
#Commenting out, we have moved this into the python OPM service
#resource "aws_api_gateway_authorizer" "parent" {
#  name                   = aws_api_gateway_rest_api.parent.name
#  rest_api_id            = aws_api_gateway_rest_api.parent.id
#  authorizer_uri         = local.authorizer_uri
#  identity_source        = "method.request.header.Authorization"
#  authorizer_result_ttl_in_seconds = 60
#  type = "REQUEST"
#}

##########################################
# Paths, PathParts
##########################################
data "aws_api_gateway_resource" "parent" {
  for_each = local.json_data.paths
  rest_api_id = aws_api_gateway_rest_api.parent.id
  path        = each.key
}

##########################################
# API Resource Method REQUEST Integrations
##########################################

resource "aws_api_gateway_integration" "parent" {
  for_each = local.final_data_model_for_api_integration
  type        = each.value["type"]
  http_method = each.value["method"]
  integration_http_method =  each.value["method"]
  passthrough_behavior = each.value["passthrough"]
  resource_id = each.value["resource_id"]
  rest_api_id = each.value["rest_api_id"]
  connection_type = each.value["connection"]
  connection_id   = each.value["connection_id"]
  timeout_milliseconds = each.value["timeout"]
  request_parameters = each.value["request_params"]
  uri = each.value["uri"]
}


###########################################
## API Gateway Deployment
###########################################
resource "aws_api_gateway_deployment" "parent" {
  rest_api_id = aws_api_gateway_rest_api.parent.id

  lifecycle {
    create_before_destroy = true
  }
  depends_on = [aws_api_gateway_integration.parent]
}

resource "aws_api_gateway_stage" "parent" {
  rest_api_id   = aws_api_gateway_rest_api.parent.id
  deployment_id = aws_api_gateway_deployment.parent.id
  stage_name    = local.deployment_stage_name
  access_log_settings {
    destination_arn = "arn:aws:logs:${local.deploment_stage_logs_region.name}:${local.deploment_stage_logs_identity.account_id}:log-group:API-Gateway-Access-Logs_${aws_api_gateway_rest_api.parent.id}/${local.deployment_stage_name}"
    format = "{ \\"requestId\\":$context.requestId,    \\"extendedRequestId\\":$context.extendedRequestId,    \\"ip\\":$context.identity.sourceIp,     \\"caller\\":$context.identity.caller,     \\"user\\":$context.identity.user,    \\"requestTime\\":$context.requestTime,    \\"httpMethod\\":$context.httpMethod,    \\"resourcePath\\":$context.resourcePath,    \\"status\\":$context.status,    \\"protocol\\":$context.protocol,    \\"responseLength\\":$context.responseLength,    \\"responseLatency\\":$context.responseLatency,    \\"apiKeyId\\":$context.identity.apiKeyId,    \\"integrationLatency\\":$context.integrationLatency,    \\"authorizeLatency\\":$context.authorize.latency,    \\"authorizerLatency\\":$context.authorizer.latency,    \\"authenticateLatency\\":$context.authenticate.latency"
    }
  variables = {    "portNumber": local.deployment_stage_var_port,    "hostName": local.deployment_stage_var_hostname,    "default4XXErrorCode": local.deployment_stage_var_4xxCodeName,    "default5XXErrorCode": local.deployment_stage_var_5xxCodeName,  }
}

resource "aws_api_gateway_method_settings" "parent" {
  method_path = "*/*"
  rest_api_id = aws_api_gateway_rest_api.parent.id
  stage_name  = aws_api_gateway_stage.parent.stage_name
  
  settings {
    metrics_enabled = true
    logging_level   = "ERROR"
    throttling_rate_limit = local.deployment_stage_setting_throttle_rate
    throttling_burst_limit = local.deployment_stage_setting_throttle_burst
  }
}

resource "aws_api_gateway_account" "parent" {
  cloudwatch_role_arn = local.api_gateway_cloudwatch_role
}

#resource "aws_iam_role" "cloudwatch" {
#  name = "api_gateway_cloudwatch_global"
#
#  assume_role_policy = <<EOF
#{
#  "Version": "2012-10-17",
#  "Statement": [
#    {
#      "Sid": "",
#      "Effect": "Allow",
#      "Principal": {
#        "Service": "apigateway.amazonaws.com"
#      },
#      "Action": "sts:AssumeRole"
#    }
#  ]
#}
#EOF
#}
#
#resource "aws_iam_role_policy" "cloudwatch" {
#  name = "default"
#  role = aws_iam_role.cloudwatch.id
#
#  policy = <<EOF
#{
#    "Version": "2012-10-17",
#    "Statement": [
#        {
#            "Effect": "Allow",
#            "Action": [
#                "logs:CreateLogGroup",
#                "logs:CreateLogStream",
#                "logs:DescribeLogGroups",
#                "logs:DescribeLogStreams",
#                "logs:PutLogEvents",
#                "logs:GetLogEvents",
#                "logs:FilterLogEvents"
#            ],
#            "Resource": "*"
#        }
#    ]
#}
#EOF
#}

'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		// println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		println results
		then:
		results.resource["aws_api_gateway_rest_api"]["parent"] != null

	}


	void "it should handle a sample aws tf template"() {
		given:
		def hcl = '''
# variables
variable "access_key" {}
variable "secret_key" {}
variable "region" {}
variable "vpc_id" {
  default = "vpc-b10f29d4"
}
variable "subnet_id" {
  default = "subnet-bcaf9ad9"
}
variable "inbound_cidr" {
  default = "0.0.0.0/0"
}

# provider
provider "aws" {
  version = "~> 2.0"
  access_key = var.access_key 
  secret_key = var.secret_key 
  region = var.region
}

# find the vpc
data "aws_vpc" "bw-vpc-1" {
  id = var.vpc_id
}

# target subnet
data "aws_subnet" "bw-subnet-1" {
  id = var.subnet_id
}

# ami
data "aws_ami" "bw-ami-1" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  # Canonical
  owners = ["099720109477"] 
}

# create a security group
resource "aws_security_group" "bw-sg-1" {
  vpc_id = var.vpc_id
  name = "bw-security-group-1"
  
  # allow ingress of port 22
  ingress {
    cidr_blocks = [var.inbound_cidr]
    from_port = 22
    to_port = 22
    protocol = "tcp"
  }
  
  # allow egress of all ports
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "bw-security-group-1"
  }
}

# create instance
resource "aws_instance" "bw-instance-1" {
  ami = data.aws_ami.bw-ami-1.id
  instance_type = "t2.micro"
  key_name = "bw-gen-key"
  security_groups = [aws_security_group.bw-sg-1.id]
  tags = {
    Name = "bw-instance-1"
  }
  subnet_id = data.aws_subnet.bw-subnet-1.id
}
'''
	HCLParser parser = new HCLParser();
	when:
	def results = parser.parse(hcl)
	println results
	then:
	results.resource["aws_instance"]["bw-instance-1"] != null

	}
}
