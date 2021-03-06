package symbolicSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPSort;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import parser.components.Variable;

class SMTSolver {
	
	Map<String, Variable> variableMap = new HashMap<String, Variable>();
	ArrayList<String> contidtions = new ArrayList<String>();
	
	Map<String, IntExpr> intVariables = new HashMap<String, IntExpr>();
	Map<String, FPExpr> floatVariables = new HashMap<String, FPExpr>();
	
	HashMap<String, String> cfg = new HashMap<String, String>();
    Context ctx = new Context(cfg);
	
    private ArrayList<Character> operators = new ArrayList<Character>( Arrays.asList('+', '-', '*', '/', '%') );
    private ArrayList<String> conditonals = new ArrayList<String>( 
    		Arrays.asList("<", "<=", ">", ">=", "==", "!=") );
    private ArrayList<String> conjuctions = new ArrayList<String>( 
    		Arrays.asList("&&", "||") );
    
	public SMTSolver() {
		cfg.put("model", "true");
	}
	
	void putValue( String name, Variable v ) {
		
		
		if( !variableMap.containsKey(name) ) variableMap.put(name, v);
		//System.out.println(variableMap.size());
	}
	
	
	String analyze() {
		
		String temp =  getCombineConditions();
		
		return solve(temp);
	}
	
	
	
	private String solve(String line) {
		
		if( line.equals("")) return ""; //when nothing to generate
		
		try {
			BoolExpr t = makeConjunctionExpression(line);
			Model model = check(ctx, t , Status.SATISFIABLE);
			
			if(model != null ) return model.toString();
			else return "Unsatisfiable";

		} catch (Exception e) {
			
		}
		
		return "Parsing Error Occured";
	}
	
	private String getCombineConditions() {
		//System.out.println(contidtions.size());
		String temp = "";
		if( contidtions.size() == 0) return "";
		
		
		for(int i=0; i<contidtions.size()-1; i++) temp += (contidtions.get(i) + " && " );
		temp += contidtions.get( contidtions.size()-1);
		//System.out.println(temp);
		return temp.trim();
	}
	
	private Model check(Context ctx, BoolExpr f, Status sat) throws Exception
    {
        Solver s = ctx.mkSolver();
        s.add(f);
        if (s.check() != sat)  return null;
        
        if (sat == Status.SATISFIABLE)
            return s.getModel();
        else
            return null;
    }

	
	private String removeOuterMostBracketContent( String line ) {
		
		int level = 0;
		
		String temp = line;
		
		if( temp.length() < 1  ||  temp.charAt(0) != '(' ) return line; // (a+b)
		
		for(int i=0; i<temp.length(); i++) {
			if( temp.charAt(i) == '(' ) level++;
			else if( temp.charAt(i) == ')') {
				level--;
				if( i != temp.length()-1 && level == 0) return line; // (a+b)*(b+4)
			}
		}
		
		//System.out.println(line.substring(1, line.length()-1));
		return line.substring(1, line.length()-1).trim();
	}
	
	private BoolExpr makeConjunctionExpression( String line ) {
		//System.out.println(line+"*");
		// !( a + 5 > 3 )
		if( ifNegativeExpression(line) == true ) {
			line = removeFalseSign(line);
			//System.out.println(line);
			return ctx.mkNot( makeConjunctionExpression(line) );
		}
		
		//System.out.println(line);
		line = removeOuterMostBracketContent(line.trim()); //remove excess outer brackets
		//System.out.println(line);
		
		//in case no more && or ||
		if( !line.contains("&&") && !line.contains("||")) return makeBooleanExpression(line);
		
		int counter = 0;
		String sign = "";
		BoolExpr temp1 = null, temp2 = null;
		
		String first = "", second ="";
		
		String words[] = line.split(" +");
		int flag = words.length;
		
		for(int i=0; i<words.length; i++) {
			
			if( words[i].equals("(") ) counter++;
			else if( words[i].equals(")") ) counter--;
			else if( counter == 0 && conjuctions.contains(words[i]) ) {
				sign = words[i];
				flag = i;
			}
		}
		
		for(int i=0; i<flag; i++) first += (words[i]+" ");
		for(int i=flag+1; i<words.length; i++) second += (words[i]+" ");
		
		//System.out.println(first+ " " + second);
		try {
			temp1 = makeConjunctionExpression( first.trim() );
			temp2 = makeConjunctionExpression( second.trim() );
		} catch (Exception e) {
			return null;
		}
		finally {
			
		}
		
		return returnConjunctionExpression(sign, temp1, temp2);
		
	}
	
	
	private BoolExpr returnConjunctionExpression(String sign, BoolExpr temp1, BoolExpr temp2) {
		
		if( sign.equals("&&") ) return ctx.mkAnd(temp1, temp2);
		else if( sign.equals("||") ) return ctx.mkOr(temp1, temp2);
		else return null;
	}
	
	private String minorParseFix(String line) {
		
		line = line.replaceAll("!", " !");
		line = line.replaceAll("! +", "!");
		
		return line;
	}
	
	private BoolExpr makeBooleanExpression( String line ) {
		
		if(line.equals("")) return null;
		//System.out.println(line+"*");
		line = removeOuterMostBracketContent(line.trim()); //remove excess outer brackets
		//System.out.println(line);
		line = minorParseFix(line);
		//System.out.println(line);
		int counter = 0;
		String sign = null;
		
		
		String first = "", second ="";
		
		String words[] = line.split(" +");
		int flag = words.length;
		
		for(int i=0; i<words.length; i++) {
			
			if( words[i].equals("(") ) counter++;
			else if( words[i].equals(")") ) counter--;
			else if( counter == 0 && conditonals.contains(words[i]) ) {
				sign = words[i];
				flag = i;
			}
		}
		
		//System.out.println(sign+"*");
		
		for(int i=0; i<flag; i++) first += (words[i]+" ");
		for(int i=flag+1; i<words.length; i++) second += (words[i]+" ");
		
		first = first.trim();
		second = second.trim();
		//System.out.println(flag);
		//System.out.println(first+ " " + second);
		
		
		try {
			if( getExpressionType(line) == 1) { 
				
				ArithExpr temp1 = null, temp2 = null;
				temp1 = makeArithmeticExpression( first.trim() );
				temp2 = makeArithmeticExpression( second.trim() );
				
				//System.out.println(temp1+ " " + temp2);
				if( temp1 == null || temp2 == null ) return null;
				return returnBoolIntExpression(sign, temp1, temp2);
			}
			else {
				FPExpr temp1 = null, temp2 = null;
				temp1 = makeFloatingExpression( first.trim() );
				temp2 = makeFloatingExpression( second.trim() );
				
				//System.out.println(temp1+ " " + temp2);
				if( temp1 == null || temp2 == null ) return null;
				return returnBoolFloatExpression(sign, temp1, temp2);
			}
		} catch (Exception e) {
			return null;
		}
		
		
		
	}
	
	private BoolExpr returnBoolFloatExpression(String sign, FPExpr temp1, FPExpr temp2) {
		//System.out.println( sign + " "+ temp1.toString() +  " "+ temp2.toString());
		if( sign.equals("<") ) return ctx.mkFPLt(temp1, temp2);
		else if( sign.equals("<=") ) return ctx.mkNot( ctx.mkFPGt(temp1, temp2) );
		else if( sign.equals(">") ) return ctx.mkFPGt(temp1, temp2);
		else if( sign.equals(">=") ) return ctx.mkNot( ctx.mkFPLt(temp1, temp2) );
		else if( sign.equals("==") ) return ctx.mkFPEq(temp1, temp2);
		else if( sign.equals("!=") ) return ctx.mkNot( ctx.mkEq(temp1, temp2) );
		else return null;
	}
	
	private BoolExpr returnBoolIntExpression(String sign, ArithExpr temp1, ArithExpr temp2) {
		//System.out.println( sign + " "+ temp1.toString() +  " "+ temp2.toString());
		if( sign.equals("<") ) return ctx.mkLt(temp1, temp2);
		else if( sign.equals("<=") ) return ctx.mkNot( ctx.mkGt(temp1, temp2) );
		else if( sign.equals(">") ) return ctx.mkGt(temp1, temp2);
		else if( sign.equals(">=") ) return ctx.mkNot( ctx.mkLt(temp1, temp2) );
		else if( sign.equals("==") ) return ctx.mkEq(temp1, temp2);
		else if( sign.equals("!=") ) return ctx.mkNot( ctx.mkEq(temp1, temp2) );
		else return null;
	}
	
private FPExpr makeFloatingExpression( String line ) {
		
		//System.out.println(line+"*");
		line = removeOuterMostBracketContent(line.trim()); //remove excess outer brackets
		line = fixMinusValue(line); // fix negative values
		//System.out.println(line);
		
		int counter = 0;
		Character sign = null;
		FPExpr temp1 = null, temp2 = null;
		
		if( line.split(" +").length == 1 ) return makeFloat( line );
		
		for(int i=0; i<line.length(); i++) {
			
			if( line.charAt(i) == '(' ) counter++;
			else if( line.charAt(i) == ')' ) counter--;
			else if( counter == 0 && operators.contains(line.charAt(i)) ) { // sign found
				sign = line.charAt(i);
				
				//System.out.println("*************");
				temp1 = makeFloatingExpression( line.substring(0, i-1));
				temp2 = makeFloatingExpression( line.substring(i+1) );
				
				return buildFloatExpression(sign, temp1, temp2);
			}
		}
		
		//make the arithmetic expression
		
		return null;
	}


	private FPExpr buildFloatExpression(Character sign, FPExpr temp1, FPExpr temp2) {
	
		//System.out.println( sign + " "+ temp1.toString());
		if( sign == '+' ) return ctx.mkFPAdd(ctx.mkFPRoundNearestTiesToAway(), temp1, temp2);
		else if( sign == '-' ) return ctx.mkFPSub(ctx.mkFPRoundNearestTiesToAway(), temp1, temp2);
		else if( sign == '*' ) return ctx.mkFPMul(ctx.mkFPRoundNearestTiesToAway(), temp1, temp2);
		else if( sign == '/' ) return ctx.mkFPDiv(ctx.mkFPRoundNearestTiesToAway(), temp1, temp2);
		else return null;
	
	}

	
	
	private ArithExpr makeArithmeticExpression( String line ) {
		
		//System.out.println(line+"*");
		line = removeOuterMostBracketContent(line.trim()); //remove excess outer brackets
		line = fixMinusValue(line); // fix negative values
		//System.out.println(line);
		
		int counter = 0;
		Character sign = null;
		ArithExpr temp1 = null, temp2 = null;
		
		if( line.split(" +").length == 1 ) return makeInteger( line );
		
		for(int i=0; i<line.length(); i++) {
			
			if( line.charAt(i) == '(' ) counter++;
			else if( line.charAt(i) == ')' ) counter--;
			else if( counter == 0 && operators.contains(line.charAt(i)) ) { // sign found
				sign = line.charAt(i);
				
				//System.out.println("*************");
				temp1 = makeArithmeticExpression( line.substring(0, i-1));
				temp2 = makeArithmeticExpression( line.substring(i+1) );
				
				return buildArithmeticExpression(sign, temp1, temp2);
			}
		}
		
		//make the arithmetic expression
		
		return null;
	}
	
	private ArithExpr buildArithmeticExpression(Character sign, ArithExpr temp1, ArithExpr temp2) {
		
		//System.out.println( sign + " "+ temp1.toString());
		if( sign == '+' ) return ctx.mkAdd(temp1, temp2);
		else if( sign == '-' ) return ctx.mkSub(temp1, temp2);
		else if( sign == '*' ) return ctx.mkMul(temp1, temp2);
		else if( sign == '/' ) return ctx.mkDiv(temp1, temp2);
		else if( sign == '%' ) return ctx.mkMod((IntExpr)temp1, (IntExpr)temp2); 
		else return null;
	}
	
	
	
	
	private FPExpr makeFloat(String word) {
		//System.out.println(word);
		word = word.trim();
		
		if( word.length() == 0 ) return null;
		else if( Character.isDigit(word.charAt(0)) || word.charAt(0) == '-' ) 
			return makeFloatConstant(word);
		else return makeFloatVariable(word);
	}
	
	private FPExpr makeFloatVariable(String variable) {
		
		if( !floatVariables.containsKey(variable) ) {
			
			FPSort s = ctx.mkFPSort(11, 53);
			FPExpr temp = (FPExpr)ctx.mkConst(ctx.mkSymbol(variable), s);
			floatVariables.put(variable, temp );
		}
		
		return floatVariables.get(variable);
	}
	
	private FPExpr makeFloatConstant(String variable) {
		
		if( !floatVariables.containsKey(variable) ) {
			FPSort s = ctx.mkFPSort(11, 53);
			FPExpr temp = ctx.mkFP( Double.parseDouble(variable) , s);
			floatVariables.put(variable, temp );
		}
		return floatVariables.get(variable);
	}
	
	
	
	private IntExpr makeInteger(String word) {
		//System.out.println(word);
		word = word.trim();
		
		if( word.length() == 0 ) return null;
		else if( Character.isDigit(word.charAt(0)) || word.charAt(0) == '-' ) 
			return makeIntConstant(word);
		else return makeIntVariable(word);
	}
	
	private IntExpr makeIntVariable(String variable) {
		
		if( !intVariables.containsKey(variable) ) {
			
			IntExpr temp = ctx.mkIntConst(variable);
			intVariables.put(variable, temp );
		}
		
		return intVariables.get(variable);
	}
	
	private IntExpr makeIntConstant(String variable) {
		
		if( !intVariables.containsKey(variable) ) {
			
			IntExpr temp = ctx.mkInt(variable);
			intVariables.put(variable, temp );
		}
		return intVariables.get(variable);
	}
	
	private String removeFalseSign(String line) {
		
		if( line.length() == 0) return line;
		
		line = line.trim();
		if( line.charAt(0) == '!' ) line = line.substring(1);
		
		//System.out.println(line);
		return line.trim();
	}
	
	private Boolean ifNegativeExpression(String line) {
		
		if(line.length() == 0) return false;
		
		line = line.trim();
		Boolean flag = false;
		
		if( line.charAt(0) == '!') {
			
			flag = true;
			line = line.substring(1); //remove !
			String words[] = line.split(" +");
			
			int counter = 0;
			for(int i=0; i<words.length; i++) {
				
				if( words[i].equals("(")) counter++;
				else if( words[i].equals(")") ) {
					
					counter--;
					if( counter == 0 && i != words.length-1 ) {
						flag = false;
						break;
					}
					
				}
				
			}
		}
		
		return flag;
	}
	
	
	private int getExpressionType(String line) {
		
		String words[] = line.split(" +");
		int maximum = 1; // we assume initially all to be integer expression
		
		for(String word: words) {
			
			if( variableMap.containsKey(word) ) {
				
				if( variableMap.get(word).getDataType().contains("float") ||
						variableMap.get(word).getDataType().contains("double")) maximum = 2;
			}
		}
		
		
		return maximum;
	}
	
	// - 2 < 5
	// a - 2 < 5
	// 3 - 4 < 5
	// 3 - a < 5
	private String fixMinusValue(String line) {
		
		String words[] = line.split(" +");
		String temp = " " + words[0];
		
		for(int i=1; i<words.length; i++) {
			
			if( words[i-1].equals("-") && Character.isDigit( words[i].charAt(0)) ) {
				temp += words[i];
			}
			else temp += (" "+words[i]);
		}
		
		return temp.trim();
	}
	
	
}
