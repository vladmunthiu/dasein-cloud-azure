package org.dasein.cloud.azure.storage.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

public class MapAdapter extends XmlAdapter<MapWrapper, Map<String, String>> {
    
	@Override
    public MapWrapper marshal(Map<String, String> m) throws Exception {
        MapWrapper wrapper = new MapWrapper();
        List<JAXBElement<String>> elements = new ArrayList<JAXBElement<String>>();
        if (m != null && !m.isEmpty()) {
	        for (Entry<String, String> property : m.entrySet()) {
	            elements.add(new JAXBElement<String>(new QName(property.getKey()), 
	                    String.class, property.getValue().toString()));
	        }
        }
        wrapper.elements = elements;
        return wrapper;
    }

    @Override
    public Map<String, String> unmarshal(MapWrapper v) throws Exception {
    	Map<String, String> map = new HashMap<String, String>();
    	if (v != null && v.elements != null && !v.elements.isEmpty()) {
	    	for (Object object : v.elements) {
	    		Element element = (Element) object;
	    		map.put(element.getNodeName(), element.getTextContent());
	    	}
    	}
    	return map;
    }

}

class MapWrapper {
    @XmlAnyElement(lax = true)
    List<JAXBElement<String>> elements;
}