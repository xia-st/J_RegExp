package pers.xia.jregexp.engine;


public class DFA
{
	Status startStatus = null;
	String input = null;
    int [] charClass = null;
    GrammerTree tree;
	
    public DFA(String input)
    {
        this.input = input;
    }
	
	Node createGrammerTree()
	{
        this.tree = new GrammerTree(this.input);
		this.tree.showTree();
        this.tree.simplify();
		this.tree.showTree();
        this.charClass = this.tree.charClass;
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
        String s = "[a-f]{1,3}2[a-cx-z]";
        DFA dfa = new DFA(s);
        dfa.createGrammerTree();
	}
	
}
