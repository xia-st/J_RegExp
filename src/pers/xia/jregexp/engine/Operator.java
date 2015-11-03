package pers.xia.jregexp.engine;

public enum Operator
{
	AND, 		//
	OR,			// |
	PLUS,		// +
	STAR,		// *
	QM, 		// ? Question Mark			
	LP,			// (
	RP,			// )
	LC,			// {
	RC,			// }
	LB,			// [
	RB,			// ]
	DOT,		// .
	
	NOT,		// 用来保存取反的情况。
}
