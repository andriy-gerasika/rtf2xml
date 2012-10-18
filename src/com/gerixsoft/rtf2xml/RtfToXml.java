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
			System.err.println("usage: <input-rtf-file-or-directory> <output-xml-file-or-directory>");
			System.exit(-1);
		}
		RtfToXml rtf2xml = new RtfToXml(true);
		rtf2xml.transform(new File(args[0]), new File(args[1]));
		System.out.println("ok");
	}

	private Validator validator;

	public RtfToXml(boolean validate) throws SAXException {
		if (validate) {
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			Schema schema = schemaFactory.newSchema(new StreamSource(RtfToXml.class.getResourceAsStream("rtf.xsd")));
			validator = schema.newValidator();
			validator.setErrorHandler(new __ErrorHandler());
		}
	}

	public void transform(File input, File output) throws IOException, RecognitionException, SAXException, TransformerConfigurationException {
		if (input.isDirectory()) {
			output.mkdir();
			for (File child : input.listFiles()) {
				if (child.isDirectory()) {
					transform(child, new File(output, child.getName()));
				} else if (child.getName().endsWith(".rtf")) {
					transform(child, new File(output, child.getName().concat(".xml")));
				}
			}
			return;
		}
		CharStream stream = new ANTLRFileStream(input.toString());
		RTFLexer lexer = new RTFLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		RTFParser parser = new RTFParser(tokenStream);
		CommonTree tree = (CommonTree) parser.rtf().getTree();

		SAXTransformerFactory handlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		TransformerHandler handler = handlerFactory.newTransformerHandler();
		handler.setResult(new StreamResult(output));
		handler.startDocument();
		try {
			TreeVisitor visitor = new TreeVisitor();
			visitor.visit(tree, new __TreeVisitorAction(handler));
		} finally {
			handler.endDocument();
		}
		if (validator != null) {
			validator.validate(new StreamSource(output));
		}
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
					// handler.processingInstruction("antlr", text);
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
