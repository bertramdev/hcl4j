package com.bertramlabs.plugins.hcl4j

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

}
