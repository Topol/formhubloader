/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.oyrm.kobo.postproc.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.oyrm.kobo.postproc.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class provides a means to generate random survey data using the template
 * of an existing, completed survey generated using OpenDataKit. The algorithm implemented
 * here isn't exactly the most flexible means of doing this. Rather than using a completed
 * survey it may be best to use the definition of a survey.
 * @author Gary Hendrick
 */
public class KoboXMLGen {
	static String USAGE = "Usage: \n\tRequires 2 Arguments \n\tKoboXMLGen <xml_file> <destination> \n\t"+
	"<xml_file> is the existing xml file to copy and fill with random values\n\t"+
	"<destination> is the directory to write generated files\n\t"+
	"<number> optional third argument, defaults to 100 indicating the number of files to generate";
	static File xmlSource, destination;
	static int number = 100;
	static List<Document> randList=new ArrayList<Document>();
	
	/**
	 * @param args the name of an XML file to duplicate and fill with
	 *            random values and the name of a storage directory to stick
	 *            these randomized files into
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws ParserConfigurationException {
		if (args.length!=2 && args.length!=3) 
			throw (new IllegalArgumentException(USAGE));
		xmlSource = new File(args[0]);
		destination = new File(args[1]);
		if(!xmlSource.exists()) throw (new IllegalArgumentException(USAGE));
		if(!destination.exists()) destination.mkdir();
		
		if(args.length==3)
			number = Integer.parseInt(args[2]);

		for (int i = 0; i < number; i++) {
			Document doc = prototype();
			randList.add(randomize(doc));
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = tFactory.newTransformer();
			DOMSource domsource = null;
			StreamResult result = null;
			File f = null;
			FileOutputStream fout = null;
			for(Document rdoc : randList) {
				String[] start = findNodeValue("start", rdoc).split("T");
				StringBuffer name = new StringBuffer();
				name.append(start[0]);
				name.append("_");
				String[] hms = start[1].split(":");
				name.append(hms[0]);
				name.append("-");
				name.append(hms[1]);
				name.append("-");
				name.append(hms[2]);
				name.append(".xml");
				f = new File(destination, name.toString()); 
				fout = new FileOutputStream(f);
				domsource = new DOMSource(rdoc);
				result = new StreamResult(fout);
				transformer.transform(domsource, result);
			}
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @return Document instance based off of the provided document example 
	 */
	public static Document prototype() {
		Document prototype = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			prototype = builder.parse( xmlSource );
		} catch (SAXParseException spe) {
			// Error generated by the parser
			System.out.println("\n** Parsing error"
					+ ", line " + spe.getLineNumber()
					+", uri " + spe.getSystemId());
			System.out.println("   " + spe.getMessage() );

			// Use the contained exception, if any
			Exception  x = spe;
			if (spe.getException() != null)
				x = spe.getException();
			x.printStackTrace();
		} catch (SAXException sxe) {
			// Error generated during parsing
			Exception  x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			x.printStackTrace();

		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			pce.printStackTrace();

		} catch (IOException ioe) {
			// I/O error
			ioe.printStackTrace();
		}
		return prototype;
	}

	public static Document randomize(Document doc) throws ParserConfigurationException {
		Node node = doc.getDocumentElement();
		NodeList list = node.getChildNodes();
		for (int i=0; i < list.getLength(); i++) {
			Node subnode = list.item(i);
			if (subnode.getNodeType() == Node.ELEMENT_NODE) {
				if(subnode.getTextContent()!=null) {
					if(subnode.getNodeName().equals("now")) {
						subnode.setTextContent(getRandomDate("2009-10-12T10:45:45.896"));
					} else if (subnode.getNodeName().endsWith(Constants.MULTI_TAG)){
						subnode.setTextContent(getMultiText());
					} else if (subnode.getNodeName().equals("geopoint")){
						String str=new  String("0123456789");
						StringBuffer sb=new StringBuffer();
						Random r = new Random();
						int te=0;
						for(int j=1;j<=35;j++){
							if(j==3 || j == 21) {
								sb.append(".");
								continue;
							} else if (j==18) {
								sb.append(" ");
								continue;
							}
							te=r.nextInt(10);
							sb.append(str.charAt(te));
						}
						subnode.setTextContent(sb.toString());
					} else if(isDateStamp(subnode.getTextContent())) {
						subnode.setTextContent(getRandomDate(subnode.getTextContent()));
					} else if(isLong(subnode.getTextContent())) {
						subnode.setTextContent(getRandomLong(subnode.getTextContent()));
					}else {
						getRandomString(subnode.getTextContent());
						subnode.setTextContent(getRandomString(subnode.getTextContent()));
					}
				}
			}
			doc.importNode(subnode, true);
		}
		return doc;
	}

	/*
	 * Make MultiSelect Text entries containing between 0 and 10 selections
	 * of the numbers 1 through 10
	 */
	private static String getMultiText() {
		StringBuffer sb = new StringBuffer();
		Random r = new Random();
		int nAnswers = Math.abs((r.nextInt())) % 10;
		for(int i = 0; i<nAnswers; i++) {
			sb.append(new Integer((Math.abs(r.nextInt()) % 10)+1).toString()+" ");
		}
		return sb.toString();
	}

	private static String getRandomLong(String textContent) {
		String str=new  String("0123456789");
		StringBuffer sb=new StringBuffer();
		Random r = new Random();
		int te=0;
		for(int i=1;i<=textContent.length();i++){
			te=r.nextInt(10);
			if(te == 0 && i ==1)
				te = 1;
			sb.append(str.charAt(te));
		}
		return sb.toString();
	}

	//TODO: find longest
	static long longest = 0;
	private static String getRandomDate(String textContent) {
		long current = System.currentTimeMillis();
		Random r = new Random();
		long offset = (Math.abs(r.nextLong())*100000) % (long)(1000*60*60*24*364);
		int exp = (offset%2 == 0 ) ? -1 : 1;
		//TODO: find longest
		longest = Math.max(Math.abs(longest), Math.abs(offset*exp));
		long randtime = (long) (current + (offset*exp));
		Date newDate = new Date(randtime);
		final Pattern ymd = Pattern.compile("20\\d\\d-[0-1]\\d-[0-3]\\d");
		final Pattern hms = Pattern.compile("[0-1]\\d:[0-6]\\d:[0-6]\\d.\\d\\d\\d");
		ymd.matcher(textContent);
		hms.matcher(textContent);
		SimpleDateFormat df;
		String date;
		if(ymd.matcher(textContent).find() && hms.matcher(textContent).find()) {
			df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.S");
		} else if(ymd.matcher(textContent).find() && !hms.matcher(textContent).find()) {
			df = new SimpleDateFormat("yyyy-MM-dd");
		} else {
			return null;
		}
		date = df.format(newDate);
		return date;
	}

	private static String getRandomString(String textContent) {
		String str=new  String("QAa 0bcLdUK2eHfJgTP8XhiFj61DOklNm9nBoI5pGqYVrs3CtSuMZvwWx4yE7zR");
		StringBuffer sb=new StringBuffer();
		Random r = new Random();
		int te=0;
		int length = r.nextInt(20);
		for(int i=0;i<=length;i++){
			te=r.nextInt(62);
			sb.append(str.charAt(te));
		}
		return sb.toString();
	}

	private static boolean isLong(String textContent) {
		try {
			Long.parseLong(textContent);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	private static boolean isDateStamp(String textContent) {
		final Pattern ymd = Pattern.compile("20\\d\\d-[0-1]\\d-[0-3]\\d");
		ymd.matcher(textContent);
		return ymd.matcher(textContent).find();
	}
	
	private static String findNodeValue(String name, Document doc) {
		Node node = doc.getDocumentElement();
		NodeList list = node.getChildNodes();
		String nodeValue = null;
		for (int i=0; i < list.getLength(); i++) {
			Node subnode = list.item(i);
			if (subnode.getNodeType() == Node.ELEMENT_NODE) {
				if(subnode.getTextContent()!=null) {
					if(subnode.getNodeName().equals(name)) {
						nodeValue = subnode.getTextContent();
					}
				}
			}
		}
		return nodeValue;
	}
}
