package org.maripo.josm.easypresets.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetNameTemplateList;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.Combo;
import org.openstreetmap.josm.gui.tagging.presets.items.ComboMultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Label;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.gui.tagging.presets.items.MultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.Text;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Container of custom presets
 * @author maripo
 *
 */
@SuppressWarnings("serial")
public class EasyPresets extends ArrayList<TaggingPreset> {
	private static final String FILE_NAME = "EasyPresets.xml";
	private static final String[] PRESET_FORMAT_URLS = {
			"https://josm.openstreetmap.de/wiki/TaggingPresets",
			"https://wiki.openstreetmap.org/wiki/Customising_JOSM_Presets"
			};
	public static final String PLUGIN_HELP_URL = "https://github.com/maripo/JOSM_easypresets/blob/master/README.md";

	boolean isDirty = false;
	/**
	 * Get file path of custom preset data file
	 * @return Full path of preset data file
	 */
	public String getXMLPath() {
		return Config.getDirs().getUserDataDirectory(true) + "/" + FILE_NAME;
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

	//List<TaggingPreset> presets = new ArrayList<TaggingPreset>();

	/**
	 * Load custom presets from local XML (if exists)
	 */
	public void load() {
		final File file = new File(EasyPresets.getInstance().getXMLPath());
		if (file.exists() && file.canRead()) {
			try (Reader reader = UTFInputStreamReader.create(new FileInputStream(file))) {
				final Collection<TaggingPreset> readResult = TaggingPresetReader.readAll(reader, true);
				if (readResult != null) {
					this.addAll(readResult);
				}
				TaggingPresets.addTaggingPresets(readResult);
			} catch (FileNotFoundException e) {
				Logging.debug("File not found: " + file.getAbsolutePath());
				return;
			} catch (SAXException | IOException e) {
				Logging.warn(e);
			}
			updatePresetListMenu();
		}
	}

	/**
	 * Add new tagging preset
	 * @param preset
	 */
	public boolean add (TaggingPreset preset) {
		super.add(preset);
		Collection<TaggingPreset> toAdd = new ArrayList<TaggingPreset>();
		toAdd.add(preset);
		// New preset will be able to find F3 menu
		TaggingPresets.addTaggingPresets(toAdd);
		return true;
	}


	/**
	 * Save all presets to specified file
	 * @param file
	 */
	public void saveAllPresetsTo(File file) {
		saveTo(this, file);
	}
	/**
	 * Save presets to specified file
	 * @param presetsToSave
	 * @param file
	 */
	public void saveTo(List<TaggingPreset> selectedPresets, File file) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("presets");
			rootElement.setAttribute("xmlns", "http://josm.openstreetmap.de/tagging-preset-1.0");
			rootElement.setAttribute("author", "");
			rootElement.setAttribute("version", "");
			rootElement.setAttribute("description", "");
			rootElement.setAttribute("shortdescription", "");
			doc.appendChild(rootElement);
			rootElement.appendChild(doc.createComment(getComment()));
			for (TaggingPreset preset: selectedPresets) {
				Element presetElement = createpresetElement(doc, preset);
				rootElement.appendChild(presetElement);
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			// Write to local XML
			StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		updatePresetListMenu();
		
	}

	private String getComment() {
		StringBuilder comment = new StringBuilder();
		comment.append("\n");
		comment.append(tr("This file is generated by EasyPresets plugin.\n"));
		comment.append(PLUGIN_HELP_URL);
		comment.append("\n");
		comment.append(tr("It supports just a few tags and attributes.\n"));
		comment.append(tr("If you want to share your custom tags with other users, please look through the official document.\n"));
		for (String url : PRESET_FORMAT_URLS) {
			comment.append(" * ");
			comment.append(url);
			comment.append("\n");
		}
		return comment.toString();
	}

	public void saveIfNeeded() {
		if (isDirty) {
			save();
		}
	}
	public void save() {
		saveAllPresetsTo(new File(EasyPresets.getInstance().getXMLPath()));
		isDirty = false;
		TaggingPresetNameTemplateList.getInstance().taggingPresetsModified();
	}
	
	private void updatePresetListMenu() {
		group.setEnabled(this.size()>0);
		group.menu.removeAll();
        for (TaggingPreset preset: this) {
            JMenuItem mi = new JMenuItem(preset);
            mi.setText(preset.getLocaleName());
            group.menu.add(mi);
        }
	}

	private TaggingPresetMenu group;
	
	/**
	 * Create a preset group holding all custom presets
	 * @return created group
	 */
	public TaggingPresetMenu createGroupMenu() {
		if (group==null) {
			group = new TaggingPresetMenu();
			group.name = tr("Custom Presets");
			group.setIcon("easypresets.png");
			JMenu menu = new JMenu(group);
			group.menu = menu;
			group.setDisplayName();
		}
		return group;
	}

	private Element createpresetElement(Document doc, TaggingPreset obj) {
		Element presetElement = doc.createElement("item");
		presetElement.setAttribute("name", obj.name);
		if (obj.iconName!=null && !obj.iconName.isEmpty()) {
			presetElement.setAttribute("icon", obj.iconName);
		}
		if (obj.types!=null && obj.types.size()>0) {
			List<String> typeNames = new ArrayList<String>();
			for (TaggingPresetType type: obj.types) {
				typeNames.add(type.getName());
			}
			presetElement.setAttribute("type", String.join(",", typeNames));
		}
		for (TaggingPresetItem item : obj.data) {
			if (item instanceof Label) {
				Label label = (Label)item;
				Element labelElement = doc.createElement("label");
				labelElement.setAttribute("text", label.text);
				presetElement.appendChild(labelElement);
			}
			else if (item instanceof Key) {
				Key key = (Key) item;
				Element keyElement = doc.createElement("key");
				keyElement.setAttribute("key", key.key);
				keyElement.setAttribute("value", key.value);
				presetElement.appendChild(keyElement);
			}
			else if (item instanceof Text) {
				Text text = (Text)item;
				Element textElement = doc.createElement("text");
				textElement.setAttribute("text", text.text);
				textElement.setAttribute("key", text.key);
				textElement.setAttribute("default", text.default_);
				presetElement.appendChild(textElement);
			}
			else if (item instanceof Combo) {
				Combo combo = (Combo)item;
				Element comboElement = doc.createElement("combo");
				comboElement.setAttribute("text", combo.text);
				comboElement.setAttribute("key", combo.key);
				comboElement.setAttribute("values", combo.values);
				presetElement.appendChild(comboElement);
			}
			else if (item instanceof MultiSelect) {
				MultiSelect multiselect = (MultiSelect)item;
				Element multiselectElement = doc.createElement("multiselect");
				multiselectElement.setAttribute("text", multiselect.text);
				multiselectElement.setAttribute("key", multiselect.key);
				multiselectElement.setAttribute("values", multiselect.values);
				presetElement.appendChild(multiselectElement);
			}
			else if (item instanceof Check) {
				Check key = (Check) item;
				Element keyElement = doc.createElement("check");
				keyElement.setAttribute("text", key.text);
				keyElement.setAttribute("key", key.key);
				presetElement.appendChild(keyElement);
			}
			else if (item instanceof Link) {
				Link link = (Link)item;
				Element linkItem = doc.createElement("link");
				linkItem.setAttribute("href", link.href);
				presetElement.appendChild(linkItem);
			}
		}
		return presetElement;
	}

	public TaggingPreset getLastItem() {
		if (this.size()==0) {
			return null;
		}
		Object[] objs = this.toArray();
		return (TaggingPreset)objs[objs.length-1];
	}

	public Collection<TaggingPreset> getPresets() {
		return this;
	}

	public void remove(TaggingPreset presetToRemove) {
		this.remove(presetToRemove);
	}

	public void delete(TaggingPreset presetToDelete) {
		remove(presetToDelete);
		save();
		updatePresetListMenu();
	}


	/**
	 * Reorder presets
	 * @param index
	 */
	public void moveDown(int index) {
		if (index >= this.size()-1) {
			return;
		}
		TaggingPreset presetToMove = this.remove(index);
		this.add(index+1, presetToMove);
		isDirty = true;
	}

	/**
	 * Reorder presets
	 * @param index
	 */
	public void moveUp(int index) {
		if (index <= 0) {
			return;
		}
		TaggingPreset presetToMove = this.remove(index);
		this.add(index-1, presetToMove);
		isDirty = true;
		
	}
	
	public String getLabelFromExistingPresets (String key) {
		Collection<TaggingPreset> existingPresets = TaggingPresets.getTaggingPresets();
		Map<String, Integer> labelCountMap = new HashMap<String, Integer>();
		for (TaggingPreset preset: existingPresets) {
			for (TaggingPresetItem _item: preset.data) {
				if (_item instanceof KeyedItem) {
					KeyedItem item = (KeyedItem)_item;
					if (key.equals(item.key)) {
						String label = getLocaleLabel(item);
						if (label!=null && !label.isEmpty() && !label.equals(key)) {
							int count;
							if (labelCountMap.containsKey(label)) {
								count = labelCountMap.get(label).intValue() + 1;
							} else {
								count = 1;
							}
							labelCountMap.put(label, count);
						}
					}
				}
			}
		}
		int maxCount = 0;
		String mostFrequentLabel = "";
		for (String label: labelCountMap.keySet()) {
			if (labelCountMap.get(label) > maxCount) {
				mostFrequentLabel = label;
				maxCount = labelCountMap.get(label); 
			}
		}
		return mostFrequentLabel;
	}

	static class DummyPresetClass extends Text {
		public static String getLocaleText(String text, String textContext){
			return getLocaleText(text, textContext, null);
		}
	}
	private String getLocaleLabel(KeyedItem _item) {
		if (_item instanceof Text) {
			Text item = (Text)_item;
			return (item.locale_text!=null)?
					item.locale_text:DummyPresetClass.getLocaleText(item.text, item.text_context);
		}
		if (_item instanceof ComboMultiSelect) {
			ComboMultiSelect item = (ComboMultiSelect)_item;
			return (item.locale_text!=null)?
					item.locale_text:DummyPresetClass.getLocaleText(item.text, item.text_context);
		}
		if (_item instanceof Check) {
			Check item = (Check)_item;
			return (item.locale_text!=null)?
					item.locale_text:DummyPresetClass.getLocaleText(item.text, item.text_context);
		}
		return null;
	}

	public TaggingPreset duplicate(TaggingPreset fromPreset) {
		int index = this.indexOf(fromPreset);
		TaggingPreset toPreset = clonePreset(fromPreset); 
		this.add(index+1, toPreset);
		return toPreset;
		
	}

	private TaggingPreset clonePreset(TaggingPreset fromPreset) {
		TaggingPreset preset = new TaggingPreset();
		preset.name = tr("Copy of {0}", fromPreset.name);
		preset.setIcon(fromPreset.iconName);
		for (TaggingPresetItem fromItem: fromPreset.data) {
			TaggingPresetItem item = clonePresetTag(fromItem);
			if (item != null) {
				preset.data.add(item);
			}
		}
		preset.types = EnumSet.noneOf(TaggingPresetType.class);
		preset.types.addAll(fromPreset.types);
		isDirty = true;
		return preset;
	}

	private TaggingPresetItem clonePresetTag(TaggingPresetItem itemFrom) {
		if (itemFrom instanceof Label) {
			Label itemTo = new Label(); 
			itemTo.text = ((Label) itemFrom).text;
			return itemTo;
		}
		else if (itemFrom instanceof Key) {
			Key key = (Key) itemFrom;
			Key itemTo = new Key();
			itemTo.key = key.key;
			itemTo.value = key.value;
			return itemTo;
		}
		else if (itemFrom instanceof Text) {
			Text text = (Text)itemFrom;
			Text itemTo = new Text();
			itemTo.text = text.text;
			itemTo.key = text.key;
			itemTo.default_ = text.default_;
			return itemTo;
		}
		else if (itemFrom instanceof Combo) {
			Combo combo = (Combo)itemFrom;
			Combo itemTo = new Combo();
			itemTo.text = combo.text;
			itemTo.key = combo.key;
			itemTo.values = combo.values;
			return itemTo;
		}
		else if (itemFrom instanceof MultiSelect) {
			MultiSelect multiselect = (MultiSelect)itemFrom;
			MultiSelect itemTo = new MultiSelect();
			itemTo.text = multiselect.text;
			itemTo.key = multiselect.key;
			itemTo.values = multiselect.values;
			return itemTo;
		}
		else if (itemFrom instanceof Check) {
			Check key = (Check) itemFrom;
			Check itemTo = new Check();
			itemTo.text = key.text;
			itemTo.key = key.key;
			return itemTo;
		}
		else if (itemFrom instanceof Link) {
			Link link = (Link)itemFrom;
			Link itemTo = new Link();
			itemTo.href = link.href;

			return itemTo;
		}
		return null;
	}
}
