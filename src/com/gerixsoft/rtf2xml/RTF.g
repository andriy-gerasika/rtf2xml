grammar RTF;

options {
  language = Java;
  output = AST;
}

tokens {
	XML_ELEMENT;
}

@lexer::header
{
package com.gerixsoft.rtf2xml;
}

@header
{
package com.gerixsoft.rtf2xml;
}

rtf : group;

group : '{' group_+ '}' -> ^(XML_ELEMENT["group"] group_+);
group_: group|control|text;

control : CODE -> ^(XML_ELEMENT["control"] CODE);
fragment CODE : '\\'{setText("");} ('a'..'z'|'A'..'Z'|'0'..'9')+;// {setText(getText().substring(2));};

text: Text;
Text: CHAR+;
fragment CHAR
    : '\u0000' .. '\u005b'
    | '\u005d' .. '\u007a'
    | '\u007c'
    | '\u007e' .. '\uffff'
    ;
