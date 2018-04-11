#!/usr/bin/env groovy
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5')

String host = 'jira'
int port = 80
if (args.length > 0)
    host = args[0]
if (args.length > 1)
    port = Integer.parseInt(args[1])

new Verification(host, port).execute()
