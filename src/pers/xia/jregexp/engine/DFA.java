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
        String s = "[a-dd-he-ha-z]";
        GrammerTree tree = new GrammerTree(s);
		tree.showTree();
        tree.simplify();
		tree.showTree();
	}
	
}
