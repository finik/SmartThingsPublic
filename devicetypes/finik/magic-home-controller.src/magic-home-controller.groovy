metadata {
	definition (name: "Magic Home Controller", namespace: "finik", author: "Dmitry Fink") {
		capability "Switch Level"
		capability "Actuator"
        capability "Polling"
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
}

def installed() {
	log.debug "installed()"
}

def refresh() {
	log.debug "refresh()"
    parent.childState(device.deviceNetworkId)
}

def poll()
{
    log.debug "poll()"
    parent.childState(device.deviceNetworkId)
}

def ping()
{
    log.debug "ping()"
    parent.childState(device.deviceNetworkId)
}

def initialize(){
    runEvery5Minutes(poll)
}

def on() {
	log.debug "on()"
    parent.childOn(device.deviceNetworkId)
}

def off() {
	log.debug "off()"
    parent.childOff(device.deviceNetworkId)
}


def setLevel(value) {
	log.debug "setLevel(${value})"
    sendEvent(name: "level", value: value)
    
    return sendColorCommand(getRed(), getGreen(), getBlue())
}

private sendColorCommand(r, g, b) {
	log.debug "sendColorCommand(${r}, ${g}, ${b})"
    
    def level = getLevel()
    def ww = 0;
    
    def red = (r * level / 100).toInteger()
    def green = (g * level / 100).toInteger()
    def blue = (b * level / 100).toInteger()
    
    if ([r, g, b].min() > 250) ww = (255 * level / 100).toInteger();
    
    log.debug "sendColorCommand setting RGB to [${red}, ${green}, ${blue}]"
    parent.childColor(device.deviceNetworkId, red, green, blue, ww)
}

def setColor(value) {
	log.debug "setColor(${value})"
    
    if ((value.red != null) && (value.green != null) && (value.blue != null)) {
    	// We got pure RGB, what else do we need?
        sendColorCommand(value.red, value.green, value.blue)
    } else if ((value.hue != null) && (value.saturation != null)) {
    	// No RGB, but we have Hue/Sat, lets calculate RGB
    	def rgb = HS2RGB(value.hue as Integer, value.saturation as Integer)
        sendColorCommand(rgb[0], rgb[1], rgb[2])
    } else if (value.hex != null) {
    	def rgb = hexToRgb(value.hex)
        sendColorCommand(rgb.r, rgb.g, rgb.b)
    }
}

def setHue(value) {
	log.debug "setHue(${value})"
    setColor([hue: value, saturation: getSat()])
}

def setSaturation(value) {
	log.debug "setSat(${value})"
    setColor([hue: getHue(), saturation: value])
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

def handleState(state) {    

    log.debug "Received state ${state}"
    sendEvent(name: "switch", value: state.power?"on":"off")
    // Adjust by current level
    def red = state.red * 100 / getLevel()
    def green = state.green * 100 / getLevel()
    def blue = state.blue * 100 / getLevel()
    sendEvent(name: "color", value: rgbToHex(red, green, blue))
    sendEvent(name: "red", value: red as Integer)
    sendEvent(name: "green", value: green as Integer)
    sendEvent(name: "blue", value: blue as Integer)

    def hs = RGB2HS(red, green, blue)
    sendEvent(name: "hue", value: hs.hue as Integer)
    sendEvent(name: "saturation", value: hs.saturation as Integer)
    
    return
}