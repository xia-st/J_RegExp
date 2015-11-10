package pers.xia.jregexp.main;
import pers.xia.jregexp.engine.*;

public class RegExpTest
{
    public static void main(String[] args)
    {
        String s = "\\d{6,11}";
        DFA dfa = new DFA(s);
        String input = "69426052618358332479";
        int begin = 0;
        int length = 0;
        while(begin < input.length())
        {
            length = dfa.matchDfa(input.substring(begin));
            if(length > 0)
            {
                System.out.println(input.substring(begin, begin + length));
                begin += length;
            }
            else
            {
                begin++;
            }
        }
    }
}
