/**
 *  Insteon Hub
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
    name: "Insteon Hub",
    namespace: "finik",
    author: "Dmitry Fink",
    description: "Insteon HUB bridge",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Hub Settings") {
        input("ip", "string", title:"Hub IP Address", description: "Insteon Hub IP Address", required: true, displayDuringSetup: true)
        input("port", "string", title:"Hub Port", description: "Insteon Hub Port", required: true, displayDuringSetup: true)
        input("username", "text", title: "Username", description: "The hub username (found in app)", required: true, displayDuringSetup: true)
        input("password", "text", title: "Password", description: "The hub password (found in app)", required: true, displayDuringSetup: true)
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
    testAndAddChildDevice("Insteon Switch", "147736", "Entrance")
    testAndAddChildDevice("Insteon Switch", "14778E", "Living Room Lights")
    
    testAndAddChildDevice("Insteon Switch", "1BAD2E", "Office Light")
    testAndAddChildDevice("Insteon Switch", "1BACE6", "Kids Room Lights")

    testAndAddChildDevice("Insteon Switch", "14A247", "Bedroom Lights")
    testAndAddChildDevice("Insteon Switch", "14A346", "Top Hallway")
    
    testAndAddChildDevice("Insteon Switch", "147872", "Stairs")
    testAndAddChildDevice("Insteon Switch", "16A1AA", "Loft")
    testAndAddChildDevice("Insteon Switch", "11DFB7", "Dining Area")
    testAndAddChildDevice("Insteon Switch", "12C37E", "Hallway")
    testAndAddChildDevice("Insteon Switch", "124F5D", "Kitchen")
    testAndAddChildDevice("Insteon Switch", "147A4D", "Bar")
    testAndAddChildDevice("Insteon Switch", "123783", "Family Room Lights")
    
    testAndAddChildDevice("Insteon Switch", "141EFB", "Plug1")
    testAndAddChildDevice("Insteon Switch", "141E5D", "Plug2")
    
    def children = getChildDevices()
    children.each { child -> child.initialize() }
}

void commandCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "commandCallback(${hubResponse.body})"
    def body = hubResponse.body
    if (body != null) {
    	log.debug "Reply from command: ${body}"
    }
}

def childOn(id) {
    log.debug "childIn(${id}})"
    sendCmd(id, 0x11, 255, commandCallback)
}

def childOff(id) {
    log.debug "childOff(${id})"
    sendCmd(id, 0x13, 0, commandCallback)
}

def childDim(id, value) {
    log.debug "childDim(${id}, ${value})"
    
    def level = (value * 255 / 100) as Integer
    level = Math.max(Math.min(level, 255), 0)

    sendCmd(id, 0x11, level, commandCallback)
}

void statusCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "statusCallback()"
    def body = hubResponse.xml
    if (body != null) {
    	def buffer = "${body}"
    	def deviceId = buffer.substring(22, 28)

        def status = buffer.substring(38, 40)

        def level = Math.round(Integer.parseInt(status, 16)*(100/255))
        log.debug "Device ${deviceId} level: ${level}"

        def children = getChildDevices()
        
        // Propagate the response to the right child
        children.each { child ->
            if (deviceId == child.deviceNetworkId) {
                child.updatestatus(level)
            }
        }
        
        if ((state.pendingDeviceId != null) && (deviceId != state.pendingDeviceId)){
            log.debug "Response is for wrong device - trying again"
            childStatus(state.pendingDeviceId)
        } else {
        	state.pendingDeviceId = null
        }
    }
}

def getBufferStatus() {
    log.debug "getBufferStatus()"
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def userpassascii = "${username}:${password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    
    def headers = [:] //"HOST:" 
	headers.put("HOST", "$ip:$port")
	headers.put("Authorization", userpass)
        
    def hubAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: "/buffstatus.xml",
        headers: headers
    ], "$iphex:$porthex",
    [callback: statusCallback])

    sendHubCommand(hubAction)    
}

def childStatus(id) {
	log.debug "childStatus(${id})"
	sendCmd(id, 0x19, 0, commandCallback)
    
    state.pendingDeviceId = id
    runIn(2, getBufferStatus)
}

private toHex(value) {
	def hex = value.toString().format( '%02x', value.toInteger() )
    return hex
}

def sendCmd(id, cmd, param, callback = null)
{
    log.debug "sendCmd(${id}, ${cmd}, ${param})"
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def userpassascii = "${username}:${password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    
    def headers = [:] //"HOST:" 
	headers.put("HOST", "$ip:$port")
	headers.put("Authorization", userpass)
    
    def uri = "/3?0262${id}0F${toHex(cmd)}${toHex(param)}=I=3"
    
    def hubAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: uri,
        headers: headers
    ], "$iphex:$porthex",
    [callback: callback])

    sendHubCommand(hubAction) 
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}