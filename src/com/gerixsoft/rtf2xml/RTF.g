grammar RTF;

options {
  language = Java;
  output = AST;
}

@lexer::header
{
package com.gerixsoft.rtf2xml;
}

@header
{
package com.gerixsoft.rtf2xml;
}

rtf : 'rtf';
