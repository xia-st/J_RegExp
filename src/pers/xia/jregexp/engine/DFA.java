package pers.xia.jregexp.engine;


public class DFA
{
	Status startStatus;
	String input;
	int subIndex;
	
	
	Node createGrammerTree()
	{
	
		return null;
	}
	
	boolean createENFA()
	{
		return true;
	}

	boolean createNFA()
	{
		createENFA();
		return true;
	}
	
	boolean createDFA()
	{
		createNFA();
		return true;
	}
	
	public static void main(String[] args)
	{
        String s = "[^1-9a-bf-g]";
        GrammerTree tree = new GrammerTree(s);
        int []charClass = new int[65536];
        tree.simplify(charClass);
		tree.showTree();
	}
	
}
