/**
 *  Insteon Hub
 *  Original Author     : ethomasii@gmail.com
 *  Creation Date       : 2013-12-08
 *
 *  Rewritten by        : idealerror
 *  Last Modified Date  : 2016-12-13
 *
 *  Rewritten by        : kuestess
 *  Last Modified Date  : 2017-12-30
 *  
 *  Rewritten by        : finik
 *  Last Modified Date  : 2018-01-27 
 */
  
metadata {
    definition (name: "Insteon Hub", author: "finik", namespace: "finik") {
        capability "Switch Level"
        capability "Switch"
        capability "Refresh"
    }

    // UI tile definitions
    tiles(scale:2) {        
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"installed", icon:"st.secondary.refresh"
        }

        main(["refresh"])
    }
    
    preferences {
        input("ip", "string", title:"Hub IP Address", description: "Insteon Hub IP Address", required: true, displayDuringSetup: true)
        input("port", "string", title:"Hub Port", description: "Insteon Hub Port", required: true, displayDuringSetup: true)
        input("username", "text", title: "Username", description: "The hub username (found in app)", required: true, displayDuringSetup: true)
        input("password", "text", title: "Password", description: "The hub password (found in app)", required: true, displayDuringSetup: true)
    }
}

void commandCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "commandCallback()..."
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
    device.deviceNetworkId = "$iphex:$porthex"

    def userpassascii = "${username}:${password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    
    def headers = [:] //"HOST:" 
	headers.put("HOST", "$ip:$port")
	headers.put("Authorization", userpass)
        
    def hubAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: "/buffstatus.xml",
        headers: headers
    ], device.deviceNetworkId,
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
    device.deviceNetworkId = "$iphex:$porthex"

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
    ], device.deviceNetworkId,
    [callback: callback])

    sendHubCommand(hubAction) 
}

def installed() {
	log.debug "installed()"
    
    addChildDevice("Insteon Switch", "147736", device.getHub().getId(), [
        label: "Entrance"
    ])
    addChildDevice("Insteon Switch", "14778E", device.getHub().getId(), [
        label: "Living Room Lights"
    ])
    addChildDevice("Insteon Switch", "141E5D", device.getHub().getId(), [
        label: "Christmass lights"
    ])
    
    addChildDevice("Insteon Switch", "1BAD2E", device.getHub().getId(), [
        label: "Office Light"
    ])
    addChildDevice("Insteon Switch", "1BACE6", device.getHub().getId(), [
        label: "Kids Room Lights"
    ])
    addChildDevice("Insteon Switch", "141EFB", device.getHub().getId(), [
        label: "Bedroom Floor"
    ])
    addChildDevice("Insteon Switch", "14A247", device.getHub().getId(), [
        label: "Bedroom Nightstand"
    ])
    addChildDevice("Insteon Switch", "14A346", device.getHub().getId(), [
        label: "Top Hallway"
    ])
    
    addChildDevice("Insteon Switch", "147872", device.getHub().getId(), [
        label: "Stairs"
    ])    
    addChildDevice("Insteon Switch", "16A1AA", device.getHub().getId(), [
        label: "Lower Office"
    ])
    addChildDevice("Insteon Switch", "11DFB7", device.getHub().getId(), [
        label: "Dining Area"
    ])
    addChildDevice("Insteon Switch", "12C37E", device.getHub().getId(), [
        label: "Hallway"
    ])
    addChildDevice("Insteon Switch", "124F5D", device.getHub().getId(), [
        label: "Kitchen"
    ])
    addChildDevice("Insteon Switch", "147A4D", device.getHub().getId(), [
        label: "Bar"
    ])
    addChildDevice("Insteon Switch", "123783", device.getHub().getId(), [
        label: "Family Room Lights"
    ])
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}