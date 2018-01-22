metadata {
	definition (name: "Magic Home Controller", namespace: "finik", author: "Dmitry Fink") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"  
     }
    
     tiles(scale: 2)  {

        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.color", key: "SECONDARY_CONTROL") {
				attributeState "color", label:'Color ${currentValue}'
			}
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"setLevel"
            }
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}
        controlTile("rgbSelector", "device.color", "color", height: 6, width: 6, inactiveLabel: false) {
    		state "color", action: "setColor"
		}
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
		}

               

        main(["switch"])
        //details(["switch", "rgbSelector"])

    }
    
    preferences {
        input("ip", "string", title:"Gateway IP Address", description: "Controller IP Address", required: true, displayDuringSetup: true)
        input("port", "string", title:"Gateway Port", description: "Controller Port", required: true, displayDuringSetup: true)
        input("id", "string", title:"Device unique ID", description: "Device unique ID", required: true, displayDuringSetup: true)
        input(name:"CStyle", type:"enum", title: "Controller Style", options: ["RGBWW", "RGBW", "RGB+WW", "RGB"], description: "Enter Controller Style", required: true, displayDuringSetup: true)
    }
}

def installed() {
	log.debug "installed()"
}

def parse(description) {
	def events = []
	log.debug "parse(${description})"
    def msg = parseLanMessage(description)
    def json = msg.json
    if (json.ok == true) {
        if (json.cmd == 'state') {
    		log.debug "Received state ${json.data}"
            events << createEvent(name: "switch", value: json.data.power?"on":"off")
            // Adjust by current level
            def red = json.data.red * 100 / getLevel()
            def green = json.data.green * 100 / getLevel()
            def blue = json.data.blue * 100 / getLevel()
            events << createEvent(name: "color", value: rgbToHex(red, green, blue))
    	} else if (json.cmd == 'color') {
        	log.debug "Color command success"
            events << createEvent(name: "color", value: rgbToHex(getRed(), getGreen(), getBlue()))
        }
    }
    
	return events
}

def refresh() {
	log.debug "refresh()"
    getAction("/state")
}

def on() {
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
    getAction("/on")
}

def off() {
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
    getAction("/off")
}

private getAction(uri) {
	log.debug "getAction(${uri})"
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$iphex:$porthex"
    log.debug "${device.deviceNetworkId}"
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/${id}${uri}"
    )
    return hubAction 
}


private sendColorCommand() {
	log.debug "sendColorCommand()"
	def hosthex = convertIPtoHex(ip);
    def porthex = convertPortToHex(port);
    def target = "$hosthex:$porthex";
    device.deviceNetworkId = target;
    
    def level = getLevel()
    
    def red = (getRed() * level / 100).toInteger()
    def green = (getGreen() * level / 100).toInteger()
    def blue = (getBlue() * level / 100).toInteger()
    
    log.debug "sendColorCommand setting RGB to [${red}, ${green}, ${blue}]"
    getAction("/color?r=${red}&g=${green}&b=${blue}")
}

def setLevel(value) {
	log.debug "setLevel(${value})"
    sendEvent(name: "level", value: value)
    
    sendColorCommand()
}

def setColor(value) {
	log.debug "setColor(${value})"
    
    if ((value.red != null) && (value.green != null) && (value.blue != null)) {
    	// We got pure RGB, what else do we need?
        // See if we need to update Hue/Sat
        def hs = RGB2HS(value.red, value.green, value.blue)
        value.hue = hs.hue
        value.saturation = hs.saturation
        
    } else if ((value.hue != null) && (value.saturation != null)) {
    	// No RGB, but we have Hue/Sat, lets calculate RGB
    	def rgb = HS2RGB(value.hue as Integer, value.saturation as Integer)
        value.red = rgb[0]
        value.green = rgb[1]
        value.blue = rgb[2]
    }
    
	sendEvent(name: "red", value: value.red)
    sendEvent(name: "green", value: value.green)
    sendEvent(name: "blue", value: value.blue)
    sendEvent(name: "hue", value: value.hue)
    sendEvent(name: "saturation", value: value.saturation)
    sendEvent(name: "color", value: rgbToHex(value.red, value.green, value.blue))
    
    sendColorCommand()
}

def setHue(value) {
	log.debug "setHue(${value})"
    sendEvent(name: "hue", value: value)
}

def setSat(value) {
	log.debug "setSat(${value})"
    sendEvent(name: "sat", value: value)
}

// handle commands
def getSaturation() {
	def valueNow = device.latestValue("saturation")
	if (valueNow == null) { 
		valueNow = 0
		sendEvent(name: "saturation", value: valueNow)
	}
	valueNow
}

def getHue() {
	def valueNow = device.latestValue("hue")
	if (valueNow == null) { 
		valueNow = 0
		sendEvent(name: "hue", value: valueNow)
	}
	valueNow
}

def getRed() {
	def valueNow = device.latestValue("red")
	if (valueNow == null) { 
		valueNow = 255
		sendEvent(name: "red", value: valueNow)
	}
	valueNow.toInteger()
}

def getGreen() {
	def valueNow = device.latestValue("green")
	if (valueNow == null) { 
		valueNow = 255
		sendEvent(name: "green", value: valueNow)
	}
	valueNow.toInteger()
}

def getBlue() {
	def valueNow = device.latestValue("blue")
	if (valueNow == null) { 
		valueNow = 255
		sendEvent(name: "blue", value: valueNow)
	}
	valueNow.toInteger()
}

def getLevel() {
	def valueNow = device.latestValue("level")
	if (valueNow == null) { 
		valueNow = 100
		sendEvent(name: "level", value: valueNow)
	}
	valueNow.toInteger()
}

def getSwitch() {
	def valueNow = device.latestValue("switch")
	if (valueNow == null) { 
		valueNow = "off"
		sendEvent(name: "switch", value: valueNow)
	}
	valueNow
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

// Return hex-string interpretation of byte array
public static String bytesToHex(byte[] bytes) {
  final char[] hexArray = "0123456789ABCDEF".toCharArray();
  char[] hexChars = new char[bytes.length * 2];
  for ( int j = 0; j < bytes.length; j++ ) {
    int v = bytes[j] & 0xFF;
    hexChars[j * 2] = hexArray[v >>> 4];
    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
  }
  return new String(hexChars);
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

def hexToRgb(colorHex) {
	def rrInt = Integer.parseInt(colorHex.substring(1,3),16)
    def ggInt = Integer.parseInt(colorHex.substring(3,5),16)
    def bbInt = Integer.parseInt(colorHex.substring(5,7),16)
    
    def colorData = [:]
    colorData = [r: rrInt, g: ggInt, b: bbInt]
    colorData
}

def rgbToHex(r, g, b) {
    def hexColor = "#${hex(r)}${hex(g)}${hex(b)}"
    hexColor
}

def HS2RGB(float hue, float sat) {
	if (hue <= 100) {
		hue = hue * 3.6
    }
    sat = sat / 100
    float v = 1.0
    float c = v * sat
    float x = c * (1 - Math.abs(((hue/60)%2) - 1))
    float m = v - c
    int mod_h = (int)(hue / 60)
    int cm = Math.round((c+m) * 255)
    int xm = Math.round((x+m) * 255)
    int zm = Math.round((0+m) * 255)
    switch(mod_h) {
    	case 0: return [cm, xm, zm]
       	case 1: return [xm, cm, zm]
        case 2: return [zm, cm, xm]
        case 3: return [zm, xm, cm]
        case 4: return [xm, zm, cm]
        case 5: return [cm, zm, xm]
	}   	
}

private RGB2HS(r, g, b) {
    float fr = r / 255f
    float fg = g / 255f
    float fb = b / 255f
    float max = [fr, fg, fb].max()
    float min = [fr, fg, fb].min()
    float delta = max - min

    float h,s,v = 0

    if (delta) {
        s = delta / max
        if (r == max) {
            h = ((fg - fb) / delta) / 6
        } else if (fg == max) {
            h = (2 + (fb - fr) / delta) / 6
        } else {
            h = (4 + (fr - fg) / delta) / 6
        }
        while (h < 0) h += 1
        while (h >= 1) h -= 1
    }
    return [ hue: h * 100, saturation: s * 100 ]
}