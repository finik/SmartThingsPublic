/**
 *  Magic Home Controller Gateway
 *
 *  Copyright 2018 Dmitry Fink
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Magic Home Controller Gateway", namespace: "finik", author: "Dmitry Fink") {
		capability "Actuator"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
	}

	tiles {
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
		}
	}
    
    preferences {
        input("ip", "string", title:"Gateway IP Address", description: "Controller IP Address", required: true, displayDuringSetup: true)
        input("port", "string", title:"Gateway Port", description: "Controller Port", required: true, displayDuringSetup: true)
    }
}

def installed() {
	log.debug "installed()"
    addChildDevice("Magic Home Controller", "bedroom", device.getHub().getId(), [
    	componentLabel: "Bedroom LED",
        label: "Bedroom LED"
    ])
    addChildDevice("Magic Home Controller", "kitchen", device.getHub().getId(), [
    	componentLabel: "Kitchen LED",
        label: "Kitchen LED"
    ])
}

def refresh() {
	childRefresh("bedroom")
}

// handle commands
def childRefresh(id) {
	log.debug "Executing 'refresh' on ${id}"
	sendHubCommand(getAction(id, "state"))
}

private getAction(id, uri) {
	log.debug "getAction(${id}, ${uri})"
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$iphex:$porthex"
    log.debug device.deviceNetworkId
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/${id}/${uri}"
    )
    log.debug "${hubAction}"
    return hubAction 
}

def childOn(id) {
	log.debug "Executing 'on' for ${id}"
    sendHubCommand(getAction(id, "on"))
}

def childOff(id) {
	log.debug "Executing 'off' for ${id}"
    sendHubCommand(getAction(id, "off"))
}

def childColor(id, red, green, blue, ww) {
	log.debug "childColor(${red}, ${green}, ${blue}, ${ww}) for ${id}"
	sendHubCommand(getAction(id, "color?r=${red}&g=${green}&b=${blue}&ww=${ww}"))
}

def parse(description) {
	def events = []
	log.debug "parse(${description})"
    def msg = parseLanMessage(description)
    def json = msg.json
    if (json.ok == true) {
    	def id = json.id
        def children = getChildDevices()
        // Propagate the response to the right child
        children.each { child ->
    		if (id == child.deviceNetworkId) {
                child.handleResponse(json)
            }
		}
        
        if (json.cmd != 'state') {
        	events << getAction(id, "state")
        }
    }
    
	return events
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}