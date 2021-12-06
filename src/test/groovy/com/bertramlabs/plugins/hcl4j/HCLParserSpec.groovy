package com.bertramlabs.plugins.hcl4j

import com.bertramlabs.plugins.hcl4j.RuntimeSymbols.*
import com.bertramlabs.plugins.hcl4j.symbols.Symbol
import groovy.json.JsonOutput
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
		results.variable.typeListString.subType instanceof StringPrimitiveType
		results.variable.typeSetNumber instanceof SetPrimitiveType
		results.variable.typeMap instanceof MapPrimitiveType
		results.variable.typeMapListString instanceof MapPrimitiveType
		results.variable.typeMapListString.subType instanceof ListPrimitiveType
		results.variable.typeMapListString.subType.subType instanceof StringPrimitiveType
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
 		results.resource.aws_instance.test.tags.Date == '${timestamp()}'
 		
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
		results.variable.fullName == '${var.firstName} ${var["lastName"]}'
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
		results.array_set.name == 'myvariable.user'
	}

	void "it should ignore complex evaluation symbols for now"() {
		given:
		def hcl = '''
		array_set = ( myvariable.name.lower(a,b) + blah.blah + 2 )
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
//		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.array_set == null

	}

	void "it should ignore complex for loops for now"() {
		given:
		def hcl = '''
output "instance_public_ip_addresses" {
  value = {
    for instance in aws_instance.example:
    instance.id => instance.public
    if instance.associate_public_ip_address
  }
}
'''
		HCLParser parser = new HCLParser();
		when:
		def results = parser.parse(hcl)
		then:
		results.output != null
	}


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
		println JsonOutput.prettyPrint(JsonOutput.toJson(results));
		then:
		results.resource["aws_launch_configuration"]["web"]?.user_data != null
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
