#!/usr/bin/env groovy
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.11')

String host = 'jira'
int port = 80
if (args.length > 0)
    host = args[0]
if (args.length > 1)
    port = Integer.parseInt(args[1])

println "Create Verification class"
new Verification(host, port, 'verification1').execute('TEST-1234')
// This does not work since mappings only handles 1 jira issue in the jira-mock (mockserver-standalone).
//println "Create second new Verification"
//new Verification(host, port, 'verification2').cancelWaitForCodeReviewToFinishScenario('TEST-1000')