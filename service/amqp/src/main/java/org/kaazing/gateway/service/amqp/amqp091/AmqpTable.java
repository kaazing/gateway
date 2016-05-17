/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.amqp.amqp091;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmqpTable {
    /**
     * AmqpTableEntry represents a single entry in the table containing
     * custom headers.
     */
    public static class AmqpTableEntry {
        private String   key;
        private Object   value;
        private AmqpType type;
        
        public AmqpTableEntry(String key, Object value, AmqpType type) {
            if ((type != AmqpType.INT)        && 
                (type != AmqpType.LONGSTRING) &&
                (type != AmqpType.FIELDTABLE) &&
                (type != AmqpType.VOID)) {
                String s = "Invalid entry type '" + type + "'. " + 
                           "Legal values are AmqpType.INT, AmqpType.LONGSTRING, and AmqpType.FIELDTABLE, and AmqpType.VOID";

                throw new IllegalStateException(s);
            }
            
            // If value is null, we will use Void as the type so that the
            // encoding and decoding can work properly.
            if (value == null) {
                type = AmqpType.VOID;
            }
            
            this.key = key;
            this.value = value;
            this.type = type;
        }
        
        /**
         * Returns the key for the entry.
         * 
         * @return
         */
        public String getKey() {
            return key;
        }
        
        /**
         * Returns the type for the entry.
         * 
         * @return
         */
        public AmqpType getType() {
            return type;
        }
        
        /**
         * Returns the value for the entry.
         * 
         * @return
         */
        public Object getValue() {
            return value;
        }
        
        /**
         * Returns the string representation of the entry.
         * 
         * @return
         */
        public String toString() {
            return "{\"key\":\"" + key + "\",\"value\":\"" + value + "\",\"type\":\"" + type + "\"}";
        }
    }

    public List<AmqpTableEntry> tableEntries = new ArrayList<>();
    
    /**
     * Returns the string/JSON representation of the table of entries.
     * 
     * @return String/JSON representation of the table of entries
     */
    public String toString() {
        String s = "{";
        int    i = 0;
        for (AmqpTableEntry entry : tableEntries) {
            s = s + "\"" + i + "\":" + entry.toString() + ",";
            i++;
        }
        
        // i has been incremented earlier so no need to
        // increment it again.
        s = s + "\"length\":" + i;
        
        return s + "}";
    }

    private void add(String key, Object value, AmqpType type) {
        AmqpTableEntry table = new AmqpTableEntry(key, value, type);
 
        if (tableEntries == null) {
            tableEntries = new ArrayList<>();
        }
        
        this.tableEntries.add(table);
    }

    /**
     * Adds an integer entry to the AmqpTable.
     *
     * @param key    name of an entry
     * @param value  integer value of an entry
     * @return AmqpTable object that holds the table of entries
     */
    public AmqpTable addInteger(String key, int value) {
        this.add(key, value, AmqpType.INT);
        return this;
    }

    /**
     * Adds a long string entry to the AmqpTable.
     *
     * @param key    name of an entry
     * @param value  long string value of an entry
     * @return AmqpTable object that holds the table of entries
     */
    public AmqpTable addLongString(String key, String value) {
        this.add(key, value, AmqpType.LONGSTRING);
        return this;
    }
    
    /**
     * Returns a list of AmqpTableEntry objects that matches the specified key.
     * If a null key is passed in, then a null is returned. Also, if the internal
     * structure is null, then a null is returned.
     * 
     * @param key    name of the entry
     * @return List<AmqpTableEntry> object with matching key
     */
    public List<AmqpTableEntry> getEntries(String key) {
        if ((key == null) || (tableEntries == null)) {
            return null;
        }
        
        List<AmqpTableEntry> entries = new ArrayList<>();
        for (AmqpTableEntry entry : tableEntries) {
            if (entry.key.equals(key)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<AmqpTableEntry> getEntries() {
        List<AmqpTableEntry> entries = new ArrayList<>();
        entries.addAll(tableEntries);
        return entries;
    }
    
    public int getLength() {
       int length = 4; // size of the table
       
       if ((tableEntries == null) || tableEntries.isEmpty()) {
           return length;
       }

       for (AmqpTableEntry entry : tableEntries) {
           String   key = entry.getKey();
           Object   value = entry.getValue();
           AmqpType type = entry.getType();
           
           // Add 1-byte for key length and length of the key.
           length += 1 + key.length();
           
           // Add 1-byte for the type identifier.
           length += 1;
           
           switch (type) {
               case LONGSTRING:
                   // Add 4-bytes to hold the length and length-number of bytes
                   // to hold the value.
                   length += 4 + ((String)value).length();
                   break;
                   
               case INT:
                   length += 4;
                   break;
                   
               case VOID:
                   break;
                   
               case FIELDTABLE:
                   length += getFieldTableLength((Map<String, Object>)value);
                   break;
                   
               default:
                   String s = "AmqpTable.getLength(): Invalid entry type '" + type + "'";
                   throw new IllegalStateException(s);
           }
       }
        
       return length;
    }
    
    public void setEntries(List<AmqpTableEntry> entries) {
        tableEntries = entries;
    }
    
    // -------------------------- Private Methods ---------------------------
    private int getFieldTableLength(Map<String, Object> fieldTable) {
        int len = 4;   // size of the field-table itself

        for (Map.Entry<String, Object> pair : fieldTable.entrySet()) {
            String key = pair.getKey();
            pair.getValue(); // Expecting this to be Boolean
            
            len += 1;                // size of the key
            len += key.length();     // key itself
            
            len += 1;                // field type
            len += 1;                // field value -- 1 for true, 0 for false
        } 

        return len;
    }
}
