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

}
