/**
 *  Magic Home Gateway
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
definition(
    name: "Magic Home Gateway",
    namespace: "finik",
    author: "Dmitry Fink",
    description: "Magic Home Gateway, requires node-magichome server",
    category: "SmartThings Labs",
    iconUrl: "https://raw.githubusercontent.com/finik/SmartThingsPublic/master/smartapps/finik/magic-home-gateway.src/icons/magichome.png",
    iconX2Url: "https://raw.githubusercontent.com/finik/SmartThingsPublic/master/smartapps/finik/magic-home-gateway.src/icons/magichome@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/finik/SmartThingsPublic/master/smartapps/finik/magic-home-gateway.src/icons/magichome@3x.png")


preferences {
	section("Gateway address") {
        input("ip", "string", title:"Gateway IP Address", description: "Controller IP Address", required: true, displayDuringSetup: true)
        input("port", "string", title:"Gateway Port", description: "Controller Port", required: true, displayDuringSetup: true)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def testAndAddChildDevice(type, id, name) {
	def device = getChildDevice(id)
    
    if (device != null) {
    	log.debug "Device ${id} already exists!"
    } else {
    	addChildDevice(type, id, null, [label: name])
    }
}

def initialize() {
    testAndAddChildDevice("Magic Home Controller", "bedroom", "Bedroom LED")
    testAndAddChildDevice("Magic Home Controller", "kitchen", "Kitchen LED")
    
    def children = getChildDevices()
    children.each { child -> child.initialize() }
}

// Status command
void stateCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "stateCallback()"
    def json = hubResponse.json
    if (json != null) {

        def children = getChildDevices()
        def deviceId = json.id
        
        // Propagate the response to the right child
        children.each { child ->
            if (deviceId == child.deviceNetworkId) {
                child.handleState(json.data)
            }
        }
    }
}

def childState(id) {
	log.debug "Executing 'refresh' on ${id}"
	sendHubCommand(getAction(id, "state", stateCallback ))
}

private getAction(id, uri, callback = null) {
	log.debug "getAction(${id}, ${uri})"
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    
    def hubAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: "/${id}/${uri}"
    ],
    "$iphex:$porthex",
    [
    	callback: callback
    ])
    return hubAction 
}


void commandCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "commandCallback()"
    def json = hubResponse.json
    if (json != null) {

        def children = getChildDevices()
        def deviceId = json.id
        
        // Trigger state retrieval
        children.each { child ->
            if (deviceId == child.deviceNetworkId) {
                childState(deviceId)
            }
        }
    }
}

def childOn(id) {
	log.debug "Executing 'on' for ${id}"
    sendHubCommand(getAction(id, "on", commandCallback))
}

def childOff(id) {
	log.debug "Executing 'off' for ${id}"
    sendHubCommand(getAction(id, "off", commandCallback))
}

def childColor(id, red, green, blue, ww) {
	log.debug "childColor(${red}, ${green}, ${blue}, ${ww}) for ${id}"
	sendHubCommand(getAction(id, "color?r=${red}&g=${green}&b=${blue}&ww=${ww}", commandCallback))
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}