package com.gerixsoft.rtf2xml;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.joost.trax.TransformerFactoryImpl;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeVisitor;
import org.antlr.runtime.tree.TreeVisitorAction;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

public class RtfToXml {

	public static void main(String[] args) throws IOException, RecognitionException, SAXException, TransformerConfigurationException {
		if (args.length != 2) {
			System.err.println("usage: <rtf-file> <xml-file>");
			System.exit(-1);
		}
		File jsonFile = new File(args[0]);
		File xmlFile = new File(args[1]);

		CharStream stream = new ANTLRFileStream(jsonFile.toString());
		RTFLexer lexer = new RTFLexer(stream);
		TokenStream input = new CommonTokenStream(lexer);
		RTFParser parser = new RTFParser(input);
		CommonTree tree = (CommonTree) parser.rtf().getTree();

		TransformerHandler handler;
		if (false) {
			SAXTransformerFactory handlerFactory = new TransformerFactoryImpl();
			handler = handlerFactory.newTransformerHandler(new StreamSource(RtfToXml.class.getResourceAsStream("rtf2xml.stx")));
		} else {
			SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			handler = handlerFactory.newTransformerHandler(new StreamSource(RtfToXml.class.getResourceAsStream("rtf2xml.xsl")));
		}
		handler.setResult(new StreamResult(xmlFile));
		handler.startDocument();
		try {
			TreeVisitor visitor = new TreeVisitor();
			visitor.visit(tree, new __TreeVisitorAction(handler));
		} finally {
			handler.endDocument();
		}

		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = factory.newSchema(new StreamSource(RtfToXml.class.getResourceAsStream("rtf.xsd")));
		Validator validator = schema.newValidator();
		validator.setErrorHandler(new __ErrorHandler());
		validator.validate(new StreamSource(xmlFile));

		System.out.println("ok");
	}

	private static final class __TreeVisitorAction implements TreeVisitorAction {
		private ContentHandler handler;

		public __TreeVisitorAction(ContentHandler handler) {
			this.handler = handler;
		}

		@Override
		public Object pre(Object o) {
			CommonTree tree = (CommonTree) o;
			Token token = tree.getToken();
			String text = token.getText();
			int type = token.getType();
			String name = RTFParser.tokenNames[type];
			try {
				if (name.equals(name.toUpperCase())) {
					handler.startElement("", text, text, new AttributesImpl());
				} else {
					//handler.processingInstruction("antlr", text);
					handler.characters(text.toCharArray(), 0, text.length());
				}
			} catch (SAXException e) {
				e.printStackTrace();
			}
			return o;
		}

		@Override
		public Object post(Object o) {
			CommonTree tree = (CommonTree) o;
			Token token = tree.getToken();
			String text = token.getText();
			int type = token.getType();
			try {
				String name = RTFParser.tokenNames[type];
				if (name.equals(name.toUpperCase())) {
					handler.endElement("", text, text);
				} else {
				}
			} catch (SAXException e) {
				e.printStackTrace();
			}
			return o;
		}

	}

	private static final class __ErrorHandler implements ErrorHandler {
		@Override
		public void warning(SAXParseException exception) throws SAXException {
			log("warning", exception);
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			log("fatal error", exception);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			log("error", exception);
		}

		public void log(String type, SAXParseException exception) {
			System.err.println(type + " at line: " + exception.getLineNumber() + " col:" + exception.getColumnNumber() + " message: " + exception.getMessage());
		}
	}

}
