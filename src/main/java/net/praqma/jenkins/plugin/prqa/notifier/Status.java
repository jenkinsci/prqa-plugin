/*
 * The MIT License
 *
 * Copyright 2012 jes.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.plugin.prqa.notifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jes
 */
public class Status implements Serializable, Comparable<Status> {

    private int messages;
    private Double fileCompliance;
    private Double projectCompliance;
    //mark list as transient, we only use for console output purposes.
    private transient List<String> notifications;

    public Status() {
       notifications = new ArrayList<String>();
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }
    
    public Double getFileCompliance() {
        return this.fileCompliance;
    }
    
    public void setFileCompliance(Double fileCompliance) {
        this.fileCompliance = fileCompliance;
    }
    
    public Double getProjectCompliance() {
        return this.projectCompliance;
    }
    
    public void setProjectCompliance(Double projCompliance) {
        this.projectCompliance = projCompliance;
    }
    
    /***
     * Implemented to provide a good reading 
     * @return 
     */
    @Override
    public String toString() {
        String out = "";       
        out += "Project Compliance Index : "+projectCompliance + "%\n";
        out += "File Compliance Index : "+fileCompliance + "%\n";
        out += "Messages : "+messages+"\n";
        for(String note : notifications) {
            out += "Notify: "+note+"\n";
        }
        return out;       
    }
    
    
    /***
     * Implemented this to decide which one is 'better than last'. We might want to later
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Status o) {
        if(o == null)
            return 1;
        if(this.projectCompliance < o.getProjectCompliance() || this.fileCompliance < o.getProjectCompliance() || this.messages > o.getMessages()) {
           return -1; 
        } else if (this.projectCompliance > o.getProjectCompliance() && this.fileCompliance > o.getFileCompliance() && this.messages < o.getMessages()) {
            return 1;
        } else {
            return 0;
        }       
    }
    
    /**
     * TODO: Where is the best place to show build messages?
     * @param message 
     */
    public void addNotication(String message) {
        notifications.add(message);
    }

}
