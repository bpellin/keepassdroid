/*
 * Copyright 2010-2022 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

import android.util.Base64;
import android.webkit.URLUtil;

import com.keepassdroid.collections.VariantDictionary;
import com.keepassdroid.crypto.CryptoUtil;
import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.crypto.engine.ChaCha20Engine;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.crypto.keyDerivation.AesKdf;
import com.keepassdroid.crypto.keyDerivation.KdfEngine;
import com.keepassdroid.crypto.keyDerivation.KdfFactory;
import com.keepassdroid.crypto.keyDerivation.KdfParameters;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Types;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class PwDatabaseV4 extends PwDatabase {

	public static final Date DEFAULT_NOW = new Date();
    public static final UUID UUID_ZERO = new UUID(0,0);
	public static final int DEFAULT_ROUNDS = 6000;
	private static final int DEFAULT_HISTORY_MAX_ITEMS = 10; // -1 unlimited
	private static final long DEFAULT_HISTORY_MAX_SIZE = 6 * 1024 * 1024; // -1 unlimited
	private static final String RECYCLEBIN_NAME = "RecycleBin";

	public byte[] hmacKey;
	public UUID dataCipher = AesEngine.CIPHER_UUID;
	public CipherEngine dataEngine = new AesEngine();
	public PwCompressionAlgorithm compressionAlgorithm = PwCompressionAlgorithm.Gzip;
	// TODO: Refactor me away to get directly from kdfParameters
    public long numKeyEncRounds = 6000;
    public Date nameChanged = DEFAULT_NOW;
    public Date settingsChanged = DEFAULT_NOW;
    public String description = "";
    public Date descriptionChanged = DEFAULT_NOW;
    public String defaultUserName = "";
    public Date defaultUserNameChanged = DEFAULT_NOW;

    public Date keyLastChanged = DEFAULT_NOW;
    public long keyChangeRecDays = -1;
    public long keyChangeForceDays = 1;
	public boolean keyChangeForceOnce = false;
    
    public long maintenanceHistoryDays = 365;
    public String color = "";
    public boolean recycleBinEnabled = true;
    public UUID recycleBinUUID = UUID_ZERO;
    public Date recycleBinChanged = DEFAULT_NOW;
    public UUID entryTemplatesGroup = UUID_ZERO;
    public Date entryTemplatesGroupChanged = DEFAULT_NOW;
    public int historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS;
    public long historyMaxSize = DEFAULT_HISTORY_MAX_SIZE;
    public UUID lastSelectedGroup = UUID_ZERO;
    public UUID lastTopVisibleGroup = UUID_ZERO;
    public MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    public List<PwDeletedObject> deletedObjects = new ArrayList<PwDeletedObject>();
    public List<PwIconCustom> customIcons = new ArrayList<PwIconCustom>();
    public PwCustomData customData = new PwCustomData();
	public KdfParameters kdfParameters = KdfFactory.getDefaultParameters();
	public VariantDictionary publicCustomData = new VariantDictionary();
	public BinaryPool binPool = new BinaryPool();
	public long version = PwDbHeaderV4.FILE_VERSION_32;

    public String localizedAppName = "KeePassDroid";
    
    public class MemoryProtectionConfig {
    	public boolean protectTitle = false;
    	public boolean protectUserName = false;
    	public boolean protectPassword = false;
    	public boolean protectUrl = false;
    	public boolean protectNotes = false;
    	
    	public boolean autoEnableVisualHiding = false;
    	
    	public boolean GetProtection(String field) {
    		if ( field.equalsIgnoreCase(PwDefsV4.TITLE_FIELD)) return protectTitle;
    		if ( field.equalsIgnoreCase(PwDefsV4.USERNAME_FIELD)) return protectUserName;
    		if ( field.equalsIgnoreCase(PwDefsV4.PASSWORD_FIELD)) return protectPassword;
    		if ( field.equalsIgnoreCase(PwDefsV4.URL_FIELD)) return protectUrl;
    		if ( field.equalsIgnoreCase(PwDefsV4.NOTES_FIELD)) return protectNotes;
    		
    		return false;
    	}
    }
    
	@Override
	public byte[] getMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {
		assert(key != null);
		
		byte[] fKey;
		
		if ( key.length() > 0 && keyInputStream != null) {
			return getCompositeKey(key, keyInputStream);
		} else if ( key.length() > 0 ) {
			fKey =  getPasswordKey(key);
		} else if ( keyInputStream != null) {
			fKey = getFileKey(keyInputStream);
		} else {
			throw new IllegalArgumentException( "Key cannot be empty." );
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

	@Override
	public void makeFinalKey(byte[] masterSeed, byte[] masterSeed2, int numRounds) throws IOException {

		byte[] transformedMasterKey = transformMasterKey(masterSeed2, masterKey, numRounds);


		byte[] cmpKey = new byte[65];
		System.arraycopy(masterSeed, 0, cmpKey, 0, 32);
		System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32);
		finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength());

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			cmpKey[64] = 1;
			hmacKey = md.digest(cmpKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-512 implementation");
		} finally {
			Arrays.fill(cmpKey, (byte)0);
		}
	}
	public void makeFinalKey(byte[] masterSeed, KdfParameters kdfP) throws IOException {
    	makeFinalKey(masterSeed, kdfP, 0);
	}

	public void makeFinalKey(byte[] masterSeed, KdfParameters kdfP, long roundsFix)
			throws IOException {

		KdfEngine kdfEngine = KdfFactory.get(kdfP.kdfUUID);
		if (kdfEngine == null) {
			throw new IOException("Unknown key derivation function");
		}

		// Set to 6000 rounds to open corrupted database
		if (roundsFix > 0 && kdfP.kdfUUID.equals(AesKdf.CIPHER_UUID)) {
			kdfP.setUInt32(AesKdf.ParamRounds, roundsFix);
			numKeyEncRounds = roundsFix;
		}

		byte[] transformedMasterKey = kdfEngine.transform(masterKey, kdfP);
		if (transformedMasterKey.length != 32) {
			transformedMasterKey = CryptoUtil.hashSha256(transformedMasterKey);
		}

        byte[] cmpKey = new byte[65];
		System.arraycopy(masterSeed, 0, cmpKey, 0, 32);
        System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32);
		finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength());

        MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			cmpKey[64] = 1;
			hmacKey = md.digest(cmpKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-512 implementation");
		} finally {
			Arrays.fill(cmpKey, (byte)0);
		}
	}

	@Override
	protected String getPasswordEncoding() {
		return "UTF-8";
	}
    
	private static final String RootElementName = "KeyFile";
	private static final String MetaElementName = "Meta";
	private static final String VersionElementName = "Version";
	private static final String KeyElementName = "Key";
	private static final String KeyDataElementName = "Data";
	
	@Override
	protected byte[] loadXmlKeyFile(InputStream keyInputStream) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(keyInputStream);
			
			Element el = doc.getDocumentElement();
			if (el == null || ! el.getNodeName().equalsIgnoreCase(RootElementName)) {
				return null;
			}
			
			NodeList children = el.getChildNodes();
			if (children.getLength() < 2) {
				return null;
			}

			long version = getVersion(children);

			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item(i);

				if ( child.getNodeName().equalsIgnoreCase(KeyElementName) ) {
					NodeList keyChildren = child.getChildNodes();
					for ( int j = 0; j < keyChildren.getLength(); j++ ) {
						Node keyChild = keyChildren.item(j);
						if ( keyChild.getNodeName().equalsIgnoreCase(KeyDataElementName) ) {
							NodeList children2 = keyChild.getChildNodes();
							for ( int k = 0; k < children2.getLength(); k++) {
								Node text = children2.item(k);
								if (text.getNodeType() == Node.TEXT_NODE) {
									Text txt = (Text) text;
									return decodeKey(txt.getNodeValue(), version);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	private byte[] decodeKey(String value, long version) {

		if (version == 0x0001000000000000L) {
			return Base64.decode(value, Base64.NO_WRAP);
		} else if (version == 0x0002000000000000L) {
			// Strip all whitespace
			value = value.replaceAll("\\s", "");
			return decodeKeyV2(value);
		} else {
			return null;
		}

	}

	private byte[] decodeKeyV2(String value) {
		if ( value == null ) {
			return null;
		}

		int len = value.length();
		if ( (len & 1) != 0) {
			return null;
		}

		byte[] pb = new byte[len / 2];

		for (int i = 0; i < len; i+= 2) {
			char ch = value.charAt(i);
			byte bt;
			bt = hexToByte(ch);

			bt <<= 4;

			ch = value.charAt((i+1));
			byte bt2 = hexToByte(ch);

			bt |= bt2;

			pb[i >> 1]  = bt;
		}

		return pb;

	}

	private byte hexToByte(char ch) {
	    byte bt;

		if ( (ch >= '0') && (ch <= '9') ) {
			bt = (byte)(ch - '0');
		} else if ((ch >= 'a') && (ch <= 'f') ) {
			bt = (byte)(ch -'a' + 10);
		} else if ( (ch >= 'A') && (ch <= 'F') ) {
			bt = (byte)(ch - 'A' + 10);
		} else {
			bt = 0;
		}

		return bt;
	}

	private long getVersion(NodeList rootChildren) {

		for (int i = 0; i < rootChildren.getLength(); i++) {
			Node child = rootChildren.item(i);

			String nodename = child.getNodeName();
			if (nodename.equalsIgnoreCase(MetaElementName)) {
				NodeList metaChildren = child.getChildNodes();
				for (int j = 0; j < metaChildren.getLength(); j++) {
					Node metaChild = metaChildren.item(j);
					if (metaChild.getNodeName().equalsIgnoreCase(VersionElementName)) {

						NodeList children2 = metaChild.getChildNodes();
						for ( int k = 0; k < children2.getLength(); k++) {
							Node text = children2.item(k);
							if (text.getNodeType() == Node.TEXT_NODE) {
								Text txt = (Text) text;
								String value = txt.getNodeValue();
								return Types.parseVersion(value);
							}
						}
					}
				}
			}
		}

		return 0;
	}

	@Override
	public List<PwGroup> getGroups() {
		List<PwGroup> list = new ArrayList<PwGroup>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		root.buildChildGroupsRecursive(list);
		
		return list;
	}

	@Override
	public List<PwGroup> getGrpRoots() {
		return rootGroup.childGroups;
	}

	@Override
	public List<PwEntry> getEntries() {
		List<PwEntry> list = new ArrayList<PwEntry>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		root.buildChildEntriesRecursive(list);
		
		return list;
	}

	@Override
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRounds(long rounds) throws NumberFormatException {
		numKeyEncRounds = rounds;
		
	}

	@Override
	public boolean appSettingsEnabled() {
		return false;
	}

	@Override
	public PwEncryptionAlgorithm getEncAlgorithm() {
		return PwEncryptionAlgorithm.Rjindal;
	}

	@Override
	public PwGroupIdV4 newGroupId() {
		PwGroupIdV4 id = new PwGroupIdV4(UUID_ZERO);
		
		while (true) {
			id = new PwGroupIdV4(UUID.randomUUID());
			
			if (!isGroupIdUsed(id)) break;
		}
		
		return id;
	}

	@Override
	public PwGroup createGroup() {
		return new PwGroupV4();
	}

	@Override
	public boolean isBackup(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		return group.isContainedIn(getRecycleBin());
	}

	@Override
	public void populateGlobals(PwGroup currentGroup) {
		groups.put(rootGroup.getId(), rootGroup);
		
		super.populateGlobals(currentGroup);
	}
	
	/** Ensure that the recycle bin group exists, if enabled and create it
	 *  if it doesn't exist 
	 *  
	 */
	private void ensureRecycleBin() {
		if (getRecycleBin() == null) {
			// Create recycle bin
				
			PwGroupV4 recycleBin = new PwGroupV4(true, true, RECYCLEBIN_NAME, iconFactory.getIcon(PwIconStandard.TRASH_BIN));
			recycleBin.enableAutoType = false;
			recycleBin.enableSearching = false;
			recycleBin.isExpanded = false;
			addGroupTo(recycleBin, rootGroup);
			
			recycleBinUUID = recycleBin.uuid;
		}
	}
	
	@Override
	public boolean canRecycle(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		PwGroup recycle = getRecycleBin();
		
		return (recycle == null) || (!group.isContainedIn(recycle));
	}

	@Override
	public boolean canRecycle(PwEntry entry) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		PwGroup parent = entry.getParent();
		return (parent != null) && canRecycle(parent);
	}
	
	@Override
	public void recycle(PwEntry entry) {
		ensureRecycleBin();
		
		PwGroup parent = entry.getParent();
		removeEntryFrom(entry, parent);
		parent.touch(false, true);
		
		PwGroup recycleBin = getRecycleBin();
		addEntryTo(entry, recycleBin);
		
		entry.touch(false, true);
		entry.touchLocation();
	}

	@Override
	public void undoRecycle(PwEntry entry, PwGroup origParent) {
		
		PwGroup recycleBin = getRecycleBin();
		removeEntryFrom(entry, recycleBin);
		
		addEntryTo(entry, origParent);
	}

	@Override
	public void deleteEntry(PwEntry entry) {
		super.deleteEntry(entry);
		
		deletedObjects.add(new PwDeletedObject(entry.getUUID()));
	}

	@Override
	public void undoDeleteEntry(PwEntry entry, PwGroup origParent) {
		super.undoDeleteEntry(entry, origParent);
		
		deletedObjects.remove(new PwDeletedObject(entry.getUUID()));
	}

	@Override
	public PwGroupV4 getRecycleBin() {
		if (recycleBinUUID == null) {
			return null;
		}
		
		PwGroupId recycleId = new PwGroupIdV4(recycleBinUUID);
		return (PwGroupV4) groups.get(recycleId);
	}

	@Override
	public boolean isGroupSearchable(PwGroup group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		
		PwGroupV4 g = (PwGroupV4) group;
		
		return g.isSearchEnabled();
	}

	@Override
	public boolean validatePasswordEncoding(String key) {
		return true;
	}

	@Override
	public void initNew(String name) {
		rootGroup = new PwGroupV4(true, true, name, iconFactory.getIcon(PwIconStandard.FOLDER));
		groups.put(rootGroup.getId(), rootGroup);
	}
	
	private String dbNameFromPath(String dbPath) {
		String filename = URLUtil.guessFileName(dbPath, null, null);
		
		if (EmptyUtils.isNullOrEmpty(filename)) {
			return "KeePass Database";
		}
		int lastExtDot = filename.lastIndexOf(".");
		if (lastExtDot == -1) {
			return filename;
		}
		
		return filename.substring(0, lastExtDot);
	}

	private class GroupGetMinVer extends GroupHandler<PwGroup> {

		public int minVer = 0;

		@Override
		public boolean operate(PwGroup group) {
            if (group == null) {
				return true;
			}
			PwGroupV4 g4 = (PwGroupV4) group;
            if (g4.tags.length() > 0) {
            	minVer = Math.max(minVer, PwDbHeaderV4.FILE_VERSION_32_4_1);
			}
			if (g4.customData.size() > 0) {
			    minVer = Math.max(minVer, PwDbHeaderV4.FILE_VERSION_32_4);
			}

			return true;
		}
	}

	private class EntryGetMinVer extends EntryHandler<PwEntry> {

	    public int minVer = 0;

		@Override
		public boolean operate(PwEntry entry) {
            if (entry == null) {
				return true;
			}

			PwEntryV4 e4 = (PwEntryV4)entry;
            if (!e4.qualityCheck) {
            	minVer = Math.max(minVer, PwDbHeaderV4.FILE_VERSION_32_4_1);
			}
			if (e4.customData.size() > 0) {
			    minVer = Math.max(minVer, PwDbHeaderV4.FILE_VERSION_32_4);
			}

			return true;
		}
	}

	public long getMinKdbxVersion() {
		long minVer = version;

		EntryGetMinVer entryHandler = new EntryGetMinVer();
		GroupGetMinVer groupHandler = new GroupGetMinVer();

		if (rootGroup != null) {
			rootGroup.preOrderTraverseTree(groupHandler, entryHandler);
			minVer = Math.max(minVer, groupHandler.minVer);
			minVer = Math.max(minVer, entryHandler.minVer);

			if (minVer >= PwDbHeaderV4.FILE_VERSION_32_4_1) {
				return minVer;
			}
		}

		for (PwIconCustom icon : customIcons) {
			if (!EmptyUtils.isNullOrEmpty(icon.name) || icon.lastMod != null) {
				return PwDbHeaderV4.FILE_VERSION_32_4_1;
			}
		}

		for (String key : customData.keySet()) {
			if (customData.getLastMod(key) != null) {
				return PwDbHeaderV4.FILE_VERSION_32_4_1;
			}
		}

		if (minVer >= PwDbHeaderV4.FILE_VERSION_32_4) {
			return minVer;
		}

		if (ChaCha20Engine.CIPHER_UUID.equals(dataCipher)) {
			return PwDbHeaderV4.FILE_VERSION_32_4;
		}

		if (!AesKdf.CIPHER_UUID.equals(kdfParameters.kdfUUID)) {
			return PwDbHeaderV4.FILE_VERSION_32_4;
		}

		if (publicCustomData.size() > 0) {
			return PwDbHeaderV4.FILE_VERSION_32_4;
		}

		return PwDbHeaderV4.FILE_VERSION_32_3;
	}

	@Override
	public void clearCache() {
		binPool.clear();
	}

}