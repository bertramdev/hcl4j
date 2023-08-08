package com.bertramlabs.plugins.hcl4j

import groovy.json.JsonOutput
import spock.lang.Specification

class HCLBaseDataLookupsSpec extends Specification {
	void "should process an http data lookup"() {
		given:

		def hcl = '''
data http "google" {
  url = "http://www.google.com"
}


resource "google1" {
	name = "google1"
	status = data.http.google.status_code
	body = data.http.google.response_body
}
resource "google2" {
	name = "google2"
	status = data.http.google.status_code
	body = data.http.google.response_body
}

'''
		HCLParser parser = new HCLParser();
		when:
		def results  = parser.parse(hcl)
		then:
		results.resource.google1.status == 200
		results.resource.google1.body.contains("google")
		results.resource.google2.status == 200
		results.resource.google2.body.contains("google")
	}
}
