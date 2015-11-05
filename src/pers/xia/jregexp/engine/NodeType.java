package pers.xia.jregexp.engine;

public enum NodeType
{
	CHAR,
	RANGE,	//XXX 虽然是个operator，但是由于不能存到value中（两个value已被占满），所以只能放到这里
	MULTICHARS,
	OPERATOR,
	METACHARACTER,
	CLASSNUM;
}
