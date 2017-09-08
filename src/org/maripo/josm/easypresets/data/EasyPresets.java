package org.maripo.josm.easypresets.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.Label;
import org.openstreetmap.josm.gui.tagging.presets.items.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Container of custom presets
 * @author maripo
 *
 */
public class EasyPresets {
	private static String FILE_NAME = "EasyPresets.xml";

	public String getXMLPath() {
		return Main.pref.getUserDataDirectory() + "/" + FILE_NAME;
	}

	private static EasyPresets instance;

	private EasyPresets() {
		super();
	}

	public static EasyPresets getInstance() {
		if (instance == null) {
			instance = new EasyPresets();
		}
		return instance;
	}

	Collection<TaggingPreset> presets = new ArrayList<TaggingPreset>();

	public void load() {
		BufferedReader bReader = null;
		FileReader fReader = null;
		try {
			fReader = new FileReader(EasyPresets.getInstance().getXMLPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		bReader = new BufferedReader(fReader);
		Collection<TaggingPreset> readResult;
		try {
			readResult = TaggingPresetReader.readAll(bReader, true);
			presets = readResult;
			TaggingPresets.addTaggingPresets(readResult);
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public void add(TaggingPreset preset) {
		presets.add(preset);
		Collection<TaggingPreset> toAdd = new ArrayList<TaggingPreset>();
		toAdd.add(preset);
		// New preset will be able to find F3 menu
		TaggingPresets.addTaggingPresets(toAdd);
	}

	public boolean save() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("presets");
			rootElement.setAttribute("xmlns", "http://josm.openstreetmap.de/tagging-preset-1.0");
			doc.appendChild(rootElement);
			for (TaggingPreset preset: presets) {
				Element presetElement = createpresetElement(doc, preset);
				rootElement.appendChild(presetElement);
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			// Write to local XML
			StreamResult result = new StreamResult(new File(EasyPresets.getInstance().getXMLPath()));
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		updatePresetListMenu();
		return true;
	}
	
	private void updatePresetListMenu() {
		// Hide if the preset list is empty
		presetContainerMenu.setVisible(presets.size()>0);
		presetContainerMenu.removeAll();
        for (TaggingPreset preset: presets) {
            JMenuItem mi = new JMenuItem(preset);
            mi.setText(preset.getLocaleName());
            presetContainerMenu.add(mi);
        }
	}

	JMenu presetContainerMenu;
	public JMenu createPresetListMenu() {
		presetContainerMenu = new JMenu("Custom Presets");
		updatePresetListMenu();
		return presetContainerMenu;
	}

	private Element createpresetElement(Document doc, TaggingPreset obj) {
		Element presetElement = doc.createElement("item");
		presetElement.setAttribute("name", obj.name);
		if (obj.types!=null && obj.types.size()>0) {
			List<String> typeNames = new ArrayList<String>();
			for (TaggingPresetType type: obj.types) {
				typeNames.add(type.getName());
			}
			presetElement.setAttribute("type", String.join(",", typeNames));
		}
		for (TaggingPresetItem item : obj.data) {
			if (item instanceof Key) {
				Key key = (Key) item;
				Element keyElement = doc.createElement("key");
				keyElement.setAttribute("key", key.key);
				keyElement.setAttribute("value", key.value);
				presetElement.appendChild(keyElement);
			}
			else if (item instanceof Label) {
				Label label = (Label)item;
				Element labelElement = doc.createElement("label");
				labelElement.setAttribute("text", label.text);
				presetElement.appendChild(labelElement);
			}
			else if (item instanceof Text) {
				Text text = (Text)item;
				Element textElement = doc.createElement("text");
				textElement.setAttribute("text", text.text);
				textElement.setAttribute("key", text.key);
				textElement.setAttribute("default", text.default_);
				presetElement.appendChild(textElement);
			}
		}
		return presetElement;
	}

	public TaggingPreset getLastItem() {
		if (presets.size()==0) {
			return null;
		}
		Object[] objs = presets.toArray();
		return (TaggingPreset)objs[objs.length-1];
	}

	public Collection<TaggingPreset> getPresets() {
		return presets;
	}

	public void remove(TaggingPreset presetToRemove) {
		presets.remove(presetToRemove);
		
	}

	public void delete(TaggingPreset presetToDelete) {
		remove(presetToDelete);
		save();
		updatePresetListMenu();
	}
}