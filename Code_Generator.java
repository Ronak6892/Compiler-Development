import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;


enum Token_type{
	ID, NUMBER, RESERVED, SYMBOL, STRING, META 
}

enum node_type{
	ID1,NUM1,MULOP1,ADDOP1
}

class ASTNode
{
	String name;
	node_type ntype;
	ASTNode parent;
	ASTNode l_node;
	ASTNode r_node;
	
	
	ASTNode()
	{
	}
	
	ASTNode(String val,node_type ntpe)
	{
		name=val;
		ntype=ntpe;
		parent=null;
		l_node=null;
		r_node=null;
	}
}


class Token
{
	Token_type ttype;
	//int state;
	String name;


	public void setToken(String value, Token_type type)
	{
		this.name=value;
		//this.state=state;
		this.ttype=type;
	}
	
	
	public Token_type getTokenType()
	{
		return this.ttype;
	}
	
	public String getTokenName()
	{
		return this.name;
	}

}


public class Code_Generator {

	/**
	 * @param args
	 */
	static FileInputStream fin=null;
	static BufferedReader br=null;
	static FileOutputStream fos=null;
	static BufferedWriter bw=null;
	static int cnt=0;
	static int len=0;
	static int cur_token=0,num_of_tokens=0;
	static int num_var=0,num_func=0,num_stmt=0;		// to store number of variables, functions, statements...
	static ArrayList<Token> tlist=new ArrayList<Token>();		// to store all generated valid tokens...
	//static HashMap<String,SymTable> func_tables=new HashMap<String,SymTable>();
	//static HashMap<String,ArrayList<Statement>> func_stmts=new HashMap<String,ArrayList<Statement>>();
	static StringBuffer curr_func=new StringBuffer();
	static StringBuffer curr_id=new StringBuffer();
	static HashMap<String,Integer> curr_stable=new HashMap<String,Integer>();
	static HashMap<String,Integer> global_stable=new HashMap<String,Integer>();
	static int curr_st_index=0;
	static int global_st_index=0;
	static ArrayList<String> curr_func_stmts=new ArrayList<String>();
	static ArrayList<String> global_stmts=new ArrayList<String>();
	static StringBuffer curr_stmt=new StringBuffer();
	static StringBuffer curr_op=new StringBuffer();
	static boolean is_global=true;
	static ASTNode curr_root=null;
	static StringBuffer curr_expr=new StringBuffer();
	static StringBuffer curr_node_name=new StringBuffer();
	static int global_var_cnt=0;
	static HashMap<String,Integer> param_table=new HashMap<String,Integer>();
	static boolean is_func_level=true;
	static int param_index=0;
	static int c_cnt=0;
	static boolean is_control_stmt=false;
	static int local_array_size=0;
	static int break_c=0;
	static int continue_c=0;
	static HashMap<String,Integer> global_array_table=new HashMap<String,Integer>();
	static HashMap<String,Integer> local_array_table=new HashMap<String,Integer>();
	static boolean while_goto_before_set=false;
	
	public static Token getNextToken(String c_line)
	{
		
		Token t=null;
		
		// check for meta-statement....
		
		if(c_line.charAt(0)=='#' || (len>=2 && c_line.substring(0,2).equals("//")))
		{
			t=new Token();
			t.setToken(c_line,Token_type.META);
			////////System.out.println("Meta detected...");
			cnt=len;
			return t;
			//tk_list.add(t);
		}
		else
		{
			
			int i=cnt,prev=cnt;
			int tk_type=-1;
			
			// check for space or tab...
			// Move ahead until all space or tab passed...
			if(c_line.charAt(i)==' ' || c_line.charAt(i)=='\t')
			{
				i++;
				while(c_line.charAt(i)==' ' || c_line.charAt(i)=='\t')
				{
					i++;
				}
			}
			
			prev=i;
			
			
			
			// comment on the same line as code.... like,
			//  foo() {     // this is foo function
			if((i+2)<len && (c_line.substring(i,i+2).equals("//")))
			{
				//////System.out.println(" inline comment...");
				t=new Token();
				t.setToken(c_line.substring(i, len),Token_type.META);
				////////System.out.println("Meta detected...:");
				cnt=len;
				return t;
			}
			// check for identifier or reserved...
			else if(Character.isLetter(c_line.charAt(i)) || c_line.charAt(i)=='_')
			{
				i++;
				while(i<len && (Character.isLetterOrDigit(c_line.charAt(i)) || c_line.charAt(i)=='_'))
				{
					i++;
				}
				
				String tmp_token=c_line.substring(prev, i);
				t=new Token();
				
				// check if scanned id is a reserved word...
				// int  | void | if | while | return | read | write | print | continue | break | binary | decimal
				
				if(tmp_token.equals("int") || tmp_token.equals("void") || tmp_token.equals("if") ||
					tmp_token.equals("while") || tmp_token.equals("return") || tmp_token.equals("read") ||
					tmp_token.equals("write") || tmp_token.equals("print") || tmp_token.equals("continue") ||
					tmp_token.equals("break") || tmp_token.equals("binary") || tmp_token.equals("decimal"))
				{
					
					////////System.out.println("RESERVED scanned == "+ tmp_token);
					t.setToken(tmp_token,Token_type.RESERVED);				// set token...
				}
				else
				{
					////////System.out.println("ID scanned =="+ tmp_token);
					
					t.setToken(tmp_token,Token_type.ID);
				}
				
				//tk_list.add(t);
				cnt=i;
			}
			
			// check for number...
			else if(Character.isDigit(c_line.charAt(i)))
			{
				i++;
				
				while(i<len && (Character.isDigit(c_line.charAt(i))))
				{
					i++;
				}
				
				String tmp_token=c_line.substring(prev, i);
				////////System.out.println("NUMBER scanned == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.NUMBER);
				//tk_list.add(t);
				cnt=i;
				
			}
			
			// check for symbol...
			// The symbols are: ( ) { } [ ] , ; + - * / == != > >= < <= = && ||)
			// 1 character long - symbol... ( ) { } [ ] , ; + - * /
			else if(c_line.charAt(i)=='(' || c_line.charAt(i)== ')' || c_line.charAt(i)== '{' ||
					c_line.charAt(i)=='}' || c_line.charAt(i)== '[' || c_line.charAt(i)== ']' ||
					c_line.charAt(i)==',' || c_line.charAt(i)== ';' || c_line.charAt(i)== '+' ||
					c_line.charAt(i)=='-' || c_line.charAt(i)== '*' || c_line.charAt(i)== '/')
			{
				i++;
				String tmp_token=c_line.substring(prev, i);
				////////System.out.println("SYMBOL scanned == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.SYMBOL);
				//tk_list.add(t);
				cnt=i;
			}
			
			// 2 character long symbol... == != >= <= && ||
			else if((i+2)<len && (c_line.substring(i,i+2).equals("==") || c_line.substring(i,i+2).equals("!=") ||
					c_line.substring(i,i+2).equals(">=") || c_line.substring(i,i+2).equals("<=") ||
					c_line.substring(i,i+2).equals("&&") || c_line.substring(i,i+2).equals("||")))
			{
				i=i+2;
				String tmp_token=c_line.substring(prev, i);
				////////System.out.println("SYMBOL scanned == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.SYMBOL);
				//tk_list.add(t);
				cnt=i;
			}
			
			// remaining 1 character symbol... < > =
			else if(c_line.charAt(i)=='<' || c_line.charAt(i)== '>' || c_line.charAt(i)== '=')
			{
				i++;
				String tmp_token=c_line.substring(prev, i);
				////////System.out.println("SYMBOL scanned == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.SYMBOL);
				//tk_list.add(t);
				cnt=i;
			}
			
			// check for string ...
			// any string between (and including) the closest pair of quotation marks. 
			else if(c_line.charAt(i)=='\"')
			{
				////////System.out.println("string case 2");
				////////System.out.println("string case");
				prev=i;
				i++;
				while(i<len && (c_line.charAt(i)!='\"'))
				{
					i++;
				}
				if(i>=len)
				{
					//////System.out.println("Input file has unfinished String line...");
					return null;
				}
				String tmp_token=c_line.substring(prev, i+1);
				////////System.out.println("STRING scanned including double quotes == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.STRING);
				//tk_list.add(t);
				i++;
				cnt=i;
			}
			else if(c_line.charAt(i)=='\'')
			{
				////////System.out.println("string case");
				prev=i;
				i++;
				while(i<len && (c_line.charAt(i)!='\''))
				{
					i++;
				}
				if(i>=len)
				{
					//////System.out.println("Input file has unfinished String line...");
					return null;
				}
				String tmp_token=c_line.substring(prev, i+1);
				////////System.out.println("STRING scanned including quotes == "+ tmp_token);
				t=new Token();
				t.setToken(tmp_token,Token_type.STRING);
				//tk_list.add(t);
				i++;
				cnt=i;
			}
			else 
			{
				////System.out.println("Input file has invalid character.");
				return null;
			}
		}
	
			return t;
	}
	
	public static int scan_file(String file_name)
	{
		try 
		{
			fin= new FileInputStream(file_name);
			//InputStreamReader isr=new InputStreamReader(fin);
			br=new BufferedReader(new InputStreamReader(fin));
			
			
			// Append "_gen" to the input file name...
			
			int dot_i=file_name.indexOf('.');
			StringBuffer out_file=new StringBuffer(file_name.substring(0,dot_i));
			
			////////System.out.println("input_file=="+args[0]+", first part="+out_file);
	
			out_file.append("_gen");
			out_file.append(file_name.substring(dot_i,file_name.length()));
			
			////////System.out.println("file output name:"+out_file);

			fos=new FileOutputStream(out_file.toString());
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			String c_line=null;
				//new StringBuffer(br.readLine().trim());

			c_line=br.readLine();
			
			while(c_line!=null)
			{
				c_line=c_line.trim();
				cnt=0;
				len=c_line.length();
				
				while(cnt<len)
				{
					////////System.out.println("cnt= "+ cnt+ " , len= "+ len+" ,c_line = " + c_line);
					StringBuffer sb=new StringBuffer();
					//sb.setLength(0);
					// Move ahead until all space or tab passed...
					int j=cnt;
					
					if(c_line.charAt(j)==' ' || c_line.charAt(j)=='\t')
					{
						j++;
						while(c_line.charAt(j)==' ' || c_line.charAt(j)=='\t')
						{
							j++;
						}
					}
					
					//sb.append(c_line.substring(cnt,j).toString());
					////////System.out.println("before get token: j="+j+" ,cnt="+cnt+", sb:"+sb.toString());
					
					Token t = getNextToken(c_line);
					if(t==null)
					{
						br.close();
						fin.close();
						
						//bw.close();
						//fos.close();
						return -1;
					}
					
					if(t.getTokenType()!= Token_type.META)
					{
						tlist.add(t);
						//sb.append(t.name + " ....... " + t.ttype.toString()+"\n");
						//////System.out.println("Token ="+t.name + " ....... " + t.ttype.toString());				
						
					}
					else if(t.getTokenType()== Token_type.META)
					{
						sb.setLength(0);
						sb.append(t.getTokenName());
						sb.append("\n");
						bw.write(sb.toString());
					}
						
					/*
					if(t.getTokenType()== Token_type.ID && !t.getTokenName().equals("main"))
					{
						sb.append("cs512");
					}
					sb.append(t.getTokenName());
					*/
					
					//sb.append(" ");
					////////System.out.println("j="+j+" ,cnt="+cnt+", sb:"+sb.toString());
					//bw.write(sb.toString());
				}
				
				//c_line.setLength(0);
				//c_line.append(br.readLine().trim());
				c_line=br.readLine();
				//c_line=c_line.trim();
			}
			
			br.close();
			fin.close();
			
			//bw.close();
			//fos.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//////System.out.println("Argument File not found...");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return 0;
	}
	
	
	// lookup global and local symbol table for ID
	// if not present in symbol table than create symbol table entry in local
	public static String get_stable_name(String id_name)
	{
		if(curr_stable.containsKey(id_name))
			return "local";
		else if (global_stable.containsKey(id_name))
			return "global";
		
		
		// create
		if(is_global)		// put in global
		{
			return "global";
		}
		else
		{
			return "local";
		}
	}
	
	public static int create_param_entry(String id_name)
	{
		if(param_table.containsKey(id_name))
			return param_table.get(id_name);
		
		//System.out.println("create_param_entry() : id=" + id_name+ " , is created at ind= "+ param_index);
		param_table.put(id_name, param_index++);
		return (param_index-1);
	}
	
	public static int create_local_entry(String id_name)
	{
		if(curr_stable.containsKey(id_name))
			return curr_stable.get(id_name);
		
		//System.out.println("create_local_entry() : id=" + id_name+ " , is created at ind= "+ curr_st_index);
		curr_stable.put(id_name, curr_st_index++);
		return (curr_st_index-1);
		
	}
	
	public static int lookup_and_create(String id_name)
	{
		//lookup
		if(curr_stable.containsKey(id_name))
			return curr_stable.get(id_name);
		else if (global_stable.containsKey(id_name))
			return global_stable.get(id_name);
		
		// create
		if(is_global)		// put in global
		{
			global_stable.put(id_name, global_st_index++);
			return (global_st_index-1);
		}
		else			// put in local
		{
		
			//System.out.println("Inside look up and create : id=" + id_name+ " , is created at ind= "+ curr_st_index);
			curr_stable.put(id_name, curr_st_index++);
			
			return (curr_st_index-1);
		}

	}
	
	// add int local[] into curr_func_stmts and then write all stmts to file...
	
	public static void decl_and_print_all()
	{
		if(is_global)
		{
			if(global_st_index>0)
			{
				String first_stmt="int global["+global_st_index+"];";
				global_stmts.add(0,first_stmt);
			}
			
			// print
			for(int i=0;i<global_stmts.size();i++)
			{
				write_file(global_stmts.get(i)+"\n");
			}
		}
		else
		{
			if(curr_st_index>0)
			{
				// add declaration stmt - "int local[];"
				String first_stmt="int local["+curr_st_index+"];";
				curr_func_stmts.add(0, first_stmt);
			}
			// print
			for(int i=0;i<curr_func_stmts.size();i++)
			{
				write_file(curr_func_stmts.get(i)+"\n");
			}
		}
		
	}
	
	public static ASTNode get_last_node(ASTNode this_top)
	{
		ASTNode tmp=this_top;
		
		while(tmp.r_node!=null)
		{
			tmp=tmp.r_node;
		}
		
		return tmp;
	}
	
	// create node for given root
	public static ASTNode create_node(ASTNode this_top,String val,node_type ntpe)
	{
		if(this_top!=null)
		{
			//System.out.println(" $$$$$$$$ this_top=" + this_top.name + " node.name = "+ val + " node_type =" + ntpe);
		}
		
		ASTNode nw_node=new ASTNode(val,ntpe);
		
		if(this_top==null)
		{
			this_top=nw_node;
		}
		else if(nw_node.ntype==node_type.MULOP1)
		{
			if(this_top.ntype==node_type.ADDOP1)
			{
				// this top should maintain as top node...
				ASTNode last_node=get_last_node(this_top);
				
				ASTNode last_parent=last_node.parent;
				
				last_parent.r_node=nw_node;
				nw_node.l_node=last_node;
				nw_node.parent=last_parent;
				last_node.parent=nw_node;
			}
			else
			{
				this_top.parent=nw_node;
				nw_node.l_node=this_top;
				this_top=nw_node;
			}
		}
		else if(nw_node.ntype==node_type.ADDOP1)
		{
			// New node is operand...
			// Add this_top as left node to nw node

			this_top.parent=nw_node;
			nw_node.l_node=this_top;
			this_top=nw_node;
		}
		else if(nw_node.ntype==node_type.NUM1 || nw_node.ntype==node_type.ID1)
		{
			//(this_top.ntype==node_type.ADDOP1 || this_top.ntype==node_type.MULOP1)
				// if this_top is operand and new node is ID or num
			// add nw node as right node of this_top
			
			ASTNode last_node=get_last_node(this_top);
			
			ASTNode last_parent=last_node.parent;
			
			if(last_node.ntype==node_type.ADDOP1 || last_node.ntype==node_type.MULOP1)
			{
				nw_node.parent=last_node;
				last_node.r_node=nw_node;
			}
			else
			{
				nw_node.parent=this_top;
				this_top.r_node=nw_node;
			}
		}

		//System.out.println("Create node: curr_func ="+ curr_func.toString() + " , curr_expr="+ curr_expr.toString() + " , node name = "+ val + " , type = " + nw_node.ntype +  ", this_top = " + this_top.name);
		if(nw_node.parent!=null)
		{
			//System.out.println(" PARENT : node = "+ nw_node.name + " , parent = " + nw_node.parent.name + ", this_top = " + this_top.name);
		}
		// return final top node...
		return this_top;
	}
	
	// get root of the tree...
	public static ASTNode get_updated_top(ASTNode node)
	{
		if(node==null)
			return node;
		
		while(node.parent!=null)
		{
			node=node.parent;
		}

		return node;
	}

	public static void print_tree(ASTNode node)
	{
		if(node==null)
			return;
		
		//System.out.println(" Tree node = " + node.name);
		if(node.parent!=null)
		{
			//System.out.println(" ####### parent = " + node.parent.name);
		}
		print_tree(node.l_node);
		print_tree(node.r_node);
	}
	
	public static String get_array_stable_name(String array_name)
	{
		if(local_array_table.containsKey(array_name))
			return "local";
		else if (global_array_table.containsKey(array_name))
			return "global";
		else
			return null;
	}
	
	public static int get_starting_index(String array_name)
	{
		if(local_array_table.containsKey(array_name))
			return local_array_table.get(array_name);
		else if (global_array_table.containsKey(array_name))
			return global_array_table.get(array_name);
		else
			return -1;
	}
	
	public static String expr_tree_walker(ASTNode node)
	{
		if(node==null)
			return null;
		
		String t3=null;
		
		if(node.ntype==node_type.ADDOP1 || node.ntype==node_type.MULOP1)
		{
			String t1=expr_tree_walker(node.l_node);
			String t2=expr_tree_walker(node.r_node);
			
			
			if(!is_global)
			{
				t3="local["+curr_st_index+"]";
				
				String new_st= t3 + " = " + t1 + " "+ node.name + " " + t2 + ";";
				curr_func_stmts.add(new_st);
				curr_stable.put(new_st, curr_st_index++);
			}
			else
			{
				t3="local["+global_st_index+"]";
				String new_st= t3 + " = " + t1 + " "+ node.name + " " + t2 + ";";
				global_stmts.add(new_st);
				global_stable.put(new_st, global_st_index++);
			}

		}
		else if(node.ntype==node_type.ID1)
		{
			int prev_st_ind;
			
			if((node.name.length()<6 || !node.name.subSequence(0,6).equals("local[")) &&
				(node.name.length()<7 || !node.name.subSequence(0,7).equals("global[")))
			{
				// todo
				// global_stable_entry in tree_walker...
				String stable_name=get_stable_name(node.name);
				
				
				if(stable_name.equals("global"))
				{
					prev_st_ind=global_st_index;
				}
				else
				{
					prev_st_ind=curr_st_index;
				}
				
				int t_id=lookup_and_create(node.name);
				
				// check if id is newly created in st table...
				if(t_id>=prev_st_ind)
				{
					String new_st=stable_name+"["+t_id+"] = " + node.name + ";";
					//curr_func_stmts.add("st_index="+prev_st_ind+", t_id match found at="+ t_id);
					curr_func_stmts.add(new_st);
				}
				
				t3=stable_name+"["+t_id+"]";
			}
			else
			{
				t3=node.name;
			}
		}
		else if(node.ntype==node_type.NUM1)
		{
			t3=node.name;
		}
		
		return t3;
	}
	

	public static void write_file(String str)
	{
		try {
			bw.write(str);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// Top level Non terminal. 
	// If this return true, input program correctly follows the given LL(1) grammar.
	public static boolean program_start()
	{
		if(cur_token==num_of_tokens)
		{
			// EOF arrived...
			////System.out.println("<program_start> parsed...: Empty file: EOF arrived::");
			return true;
		}
		
		////System.out.println("INSIDE program_start(): cur_token:" + tlist.get(cur_token).name);
		
		if(program())
		{
			////System.out.println("INSIDE program_start(): <program> parsed... ");
					//"cur_token:" + tlist.get(cur_token).name);
			if(cur_token == num_of_tokens)
			{
				//EOF arrived...
				////System.out.println("<program_start> parsed...EOF reached - SUCCESS !!!");
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean program()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE program(): EOF arrived: return FALSE:");
			return false;
		}
		
		if(type_name())
		{
			////System.out.println("INSIDE program(): <type_name> parsed... cur_token:" + tlist.get(cur_token).name );
			
			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				// New ID found, it can be global ID name or function name...
				curr_id.setLength(0);
				curr_id.append(tlist.get(cur_token).name);
				//System.out.println("program(): curr_id =" + curr_id.toString());
				
				//#1
				//curr_op.setLength(0);
				curr_op.append(curr_id.toString());
				//write_file(curr_op.toString());
				
				cur_token++;
				////System.out.println("INSIDE program():<type_name> ID parsed ... ");
						//"cur_token:" + tlist.get(cur_token).name );

				if(program_0())
				{
					////System.out.println("<program> parsed ...");
					return true;
				}
				cur_token--;

				return false;
			}
			
			return false;
		}
		
		return false;	
	}
	
	public static boolean type_name()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE type_name(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println(" INSIDE type_name() : cur_token:" + tlist.get(cur_token).name );
	
		if(tlist.get(cur_token).name.equals("int") || tlist.get(cur_token).name.equals("void")
				|| tlist.get(cur_token).name.equals("binary") || tlist.get(cur_token).name.equals("decimal"))
		{
			//#1
			curr_op.setLength(0);
			curr_op.append(tlist.get(cur_token).name+" ");
			/*
			if(is_global)
				write_file(curr_op.toString() + " ");
			*/
			
			cur_token++;
			////System.out.println("<type_name> parsed...");
			
			return true;
		}
		return false;
	}
	
	public static boolean program_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE program_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("inside <program_0> ... cur_token:" + tlist.get(cur_token).name );
		
		if(id_0())
		{
			////System.out.println("inside <program_0>: <id_0> parsed ... cur_token:" + tlist.get(cur_token).name );
			
			curr_op.setLength(0);
			num_var++;
			
			if(id_list_0())
			{
				////System.out.println("inside <program_0>: <id_0> <id_list_0> parsed... cur_token:" + tlist.get(cur_token).name );

				if(tlist.get(cur_token).name.equals(";"))
				{
					cur_token++;
					////System.out.println("inside <program_0>: <id_0> <id_list_0> ; parsed... cur_token:" + tlist.get(cur_token).name );

					if(program_1())
					{
						////System.out.println("<program_0> parsed...");

						return true;
					}
					
					cur_token--;
					return false;
				}
				return false;
			}
			
			return false;
		}
		else if(tlist.get(cur_token).name.equals("("))
		{
			////System.out.println("INSIDE program_0(): ( parsed... cur_token:" + tlist.get(cur_token).name );
			
			//#1
			curr_op.append("(");
			write_file(curr_op.toString());

			cur_token++;
			
			if(func_0())
			{
				////System.out.println("INSIDE program_0(): ( <func_0> parsed...");
						//" cur_token:" + tlist.get(cur_token).name );
				
				//num_func++;
				
				if(func_path())
				{
					////System.out.println("<program_0> parsed...");

					return true;
				}
				
				return false;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean id_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE id_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE id_0(): parsed... cur_token:" + tlist.get(cur_token).name );

		if(tlist.get(cur_token).name.equals("["))
		{
			cur_token++;
			////System.out.println("INSIDE id_0(): [ parsed... cur_token:" + tlist.get(cur_token).name );
			
			// todo...count global array size...
			
			int array_size=0;
			String prev_id=curr_id.toString();
			
			if(expression(curr_root))
			{

				////System.out.println("INSIDE id_0(): [ <expression> parsed... cur_token:" + tlist.get(cur_token).name );

				//System.out.println(" ########### array TREE is :::::::: ");
				print_tree(curr_root);
				
				String expr_str=expr_tree_walker(curr_root);
				//System.out.println("expression for global array size is="+expr_str+ ", prev_id="+prev_id);
				//curr_stmt.append(expr_str);
				
				try{
					array_size=Integer.valueOf(expr_str);
					local_array_size=array_size;
				} catch(NumberFormatException ne)
				{
					ne.printStackTrace();
				}
				
				curr_root=null;
				
				if(tlist.get(cur_token).name.equals("]"))
				{
					////System.out.println("<id_0> parsed...");
					
					if(is_global)
					{
						global_array_table.put(prev_id, global_st_index);
						for(int i=0;i<array_size;i++)
						{
							String g_key=prev_id+"["+i+"]";
							global_stable.put(g_key,global_st_index++);
							//System.out.println("global_stable_entry : id ="+g_key+ " , index="+(global_st_index-1));
							global_var_cnt++;
						}
						array_size=0;
						local_array_size=0;
					}
					else
					{
						/*
						for(int i=0;i<array_size;i++)
						{
							String g_key=prev_id+"["+i+"]";
						}
						*/
					}
					
					
					cur_token++;
					return true;
				}
				return false;
			}
			
			cur_token--;
			return false;
		}
		
		else if(tlist.get(cur_token).name.equals(",") || tlist.get(cur_token).name.equals(";"))
		{
			////System.out.println("INSIDE id_0(): Empty parsed...");
			if(is_global)
			{
				global_stable.put(curr_id.toString(),global_st_index++);
				//System.out.println("global_stable_entry : id ="+curr_id.toString()+ " , index="+(global_st_index-1));
				global_var_cnt++;
			}
			return true;
		}
		return false;
	}
	
	public static boolean id_list_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE id_list_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE id_list_0(): parsed... cur_token:" + tlist.get(cur_token).name );

		if(tlist.get(cur_token).name.equals(","))
		{
			cur_token++;
			////System.out.println("INSIDE id_list_0(): , parsed... cur_token:" + tlist.get(cur_token).name );

			if(id())
			{
				//System.out.println("INSIDE id_list_0(): , ID parsed... cur_token:" + tlist.get(cur_token).name );
				
				if(!is_global)
				{
					// #3
					//System.out.println("INSIDE id_list_0(): , ID parsed... cur_token:" + tlist.get(cur_token).name +"curr type="+ curr_op.toString()+", curr_id="+curr_id.toString());
					
					if(local_array_size<=0)
					{
						int decl_ind=create_local_entry(curr_id.toString());
						//System.out.println("local entry : index= "+ decl_ind+" , id= "+ curr_id.toString());
					}
					else
					{
						//System.out.println("Local array declrearion : array name ="+ curr_id.toString()+", size="+ local_array_size);
						
						local_array_table.put(curr_id.toString(),curr_st_index);
						for(int i=0;i<local_array_size;i++)
						{
							String l_key=curr_id.toString()+"["+i+"]";
							int decl_ind=create_local_entry(l_key);
							//System.out.println("local entry : index= "+ decl_ind+" , id= "+ l_key);
						}
						
						local_array_size=0;
					}
				}
				
				if(id_list_0())
				{
					////System.out.println("<id_list_0> parsed...");

					return true;
				}
				
				return false;
			}
			
			cur_token--;
			return false;
		}
		
		////System.out.println("<id_list_0> : Empty parsed...");

		return true;
	}
	
	public static boolean program_1()
	{
		////System.out.println("INSIDE program_1():... cur_token:" + tlist.get(cur_token).name );

		if(type_name())
		{
			////System.out.println("INSIDE program_1(): <type_name> parsed... cur_token:" + tlist.get(cur_token).name );

			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				// New ID found, it can be global ID name or function name...
				curr_id.setLength(0);
				curr_id.append(tlist.get(cur_token).name);
				//System.out.println("program_1(): curr_id =" + curr_id.toString());
				
				//#1
				curr_op.append(curr_id.toString());
				
				cur_token++;
				////System.out.println("INSIDE program_1(): <type_name> ID parsed... cur_token:" + tlist.get(cur_token).name );

				if(func_or_data())
				{
					////System.out.println("<program_1> parsed...");

					return true;
				}
				
				cur_token--;
				return false;
			}
			
			return false;
		}
		
		if(cur_token == num_of_tokens)
		{
			////System.out.println("<program_1> : EMPTY parsed... ");
			return true;
		}
		
		return false;
	}
	
	public static boolean func_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_0(): parsed... cur_token:" + tlist.get(cur_token).name );
		
		// #5
		// it's function.. need to clear param_table to store new set of parameters...
		is_func_level=true;
		param_table.clear();
		param_index=0;
		
		if(parameter_list())
		{
			////System.out.println("INSIDE func_0(): <paramter_list> parsed... cur_token:" + tlist.get(cur_token).name );

			if(tlist.get(cur_token).name.equals(")"))
			{
				//#1
				write_file(")");
				
				// #5
				// param_table entry job is finished...
				is_func_level=false;
				
				cur_token++;
				////System.out.println("INSIDE func_0(): <paramter_list> ) parsed... cur_token:" + tlist.get(cur_token).name );
	
				if(func_1())
				{
					////System.out.println("<func_0> : parsed...");

					return true;
				}
				
				cur_token--;
				
				return false;
			}
			
			return false;
		}
		else if(tlist.get(cur_token).name.equals(")"))
		{
			//#1
			write_file(")");
			
			// #5
			// param_table entry job is finished...
			is_func_level=false;
			
			cur_token++;
			////System.out.println("INSIDE func_0(): ) parsed... cur_token:" + tlist.get(cur_token).name );
			
			if(func_4())
			{
				////System.out.println("<func_0> parsed...");

				return true;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean func_path()
	{
		////System.out.println("INSIDE func_path(): parsed... ");
				//"cur_token:" + tlist.get(cur_token).name );

		if(func_list())
		{
			////System.out.println("<func_path> parsed...");

			return true;
		}
		
		if(cur_token==num_of_tokens)
		{
			////System.out.println("<func_path> : EMPTY parsed...");
			return true;
		}
		
		return false;
	}
	
	public static boolean expression(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE expression(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE expression(): parsed... cur_token:" + tlist.get(cur_token).name );
		curr_node_name.setLength(0);
		//curr_expr.setLength(0);
		
		if(factor(this_top))
		{
			////System.out.println("INSIDE expression():<factor> parsed... cur_token:" + tlist.get(cur_token).name );
			
			this_top=get_updated_top(curr_root);
			
			//System.out.println("EXpressin + factor : ");
			
			if(term_0(this_top))
			{
				////System.out.println("INSIDE expression():<factor><term_0> parsed... cur_token:" + tlist.get(cur_token).name );
				
				this_top=get_updated_top(curr_root);
				
				if(expression_0(this_top))
				{
					////System.out.println("<expression()> parsed...");
					return true;
				}
				return false;
			}
			
			return false;
		}
		
		return false;
	}
	
	public static boolean id()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE id(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE id(): parsed... cur_token:" + tlist.get(cur_token).name );

		if(tlist.get(cur_token).ttype==Token_type.ID)
		{
			// new ID found in id()...
			curr_id.setLength(0);
			curr_id.append(tlist.get(cur_token).name);
			//System.out.println("id(): curr_id =" + curr_id.toString());
			
			cur_token++;
			////System.out.println("INSIDE id(): ID parsed... cur_token:" + tlist.get(cur_token).name );

			if(id_0())
			{
				////System.out.println("<id> : parsed... ");
				num_var++;
				return true;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean func_or_data()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_or_data(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_or_data(): parsed... cur_token:" + tlist.get(cur_token).name );

		if(id_0())
		{
			////System.out.println("INSIDE func_or_data(): <id_0> parsed... cur_token:" + tlist.get(cur_token).name );
			curr_op.setLength(0);
			
			num_var++;
			if(id_list_0())
			{
				////System.out.println("INSIDE func_or_data(): <id_0> <id_list_0> parsed... cur_token:" + tlist.get(cur_token).name );

				if(tlist.get(cur_token).name.equals(";"))
				{
					cur_token++;
					////System.out.println("INSIDE func_or_data(): <id_0> <id_list_0> ; parsed... cur_token:" + tlist.get(cur_token).name );

					if(program_1())
					{
						////System.out.println("<func_or_data> parsed...");

						return true;
					}
					cur_token--;
					
					return false;
				}
				
				return false;
			}
			
			return false;
		}
		else if(tlist.get(cur_token).name.equals("("))
		{
			//System.out.println(" func_or_data(): inside function...");
			//#1
			curr_op.append("(");
			if(global_var_cnt>0)
			{
				write_file("int global["+global_var_cnt+"];\n");
			}
			write_file(curr_op.toString());
			cur_token++;
			////System.out.println("INSIDE func_or_data():( parsed... cur_token:" + tlist.get(cur_token).name );

			if(func_0())
			{
				////System.out.println("INSIDE func_or_data():( <func_0> parsed...");
						//" cur_token:" + tlist.get(cur_token).name );
				
				//num_func++;
				
				if(func_list_0())
				{
					////System.out.println("<func_or_data> parsed...");

					return true;
				}
				
				return false;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean parameter_list()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE parameter_list(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE parameter_list(): parsed... cur_token:" + tlist.get(cur_token).name);
		
		if(tlist.get(cur_token).name.equals("void"))
		{
			//#1
			write_file("void");
			cur_token++;
			////System.out.println("INSIDE parameter_list(): void parsed... cur_token:" + tlist.get(cur_token).name);

			if(parameter_list_0())
			{
				////System.out.println("<parameter_list> parsed...");
				return true;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("int"))
		{
			//#1
			write_file("int ");
			cur_token++;
			////System.out.println("INSIDE parameter_list(): int parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				//#1
				write_file(tlist.get(cur_token).name);
				
				// #5
				if(is_func_level)
				{
					int param_ind=create_param_entry(tlist.get(cur_token).name);
					//System.out.println("parameter_list(): param ="+tlist.get(cur_token).name+", is created at param ind="+param_ind);
				}
				
				cur_token++;
				////System.out.println("INSIDE parameter_list(): int ID parsed... cur_token:" + tlist.get(cur_token).name);

				if(non_empty_list_0())
				{
					////System.out.println("<parameter_list> parsed...");
					return true;
				}
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("binary"))
		{
			//#1
			write_file("binary ");
			
			cur_token++;
			////System.out.println("INSIDE parameter_list(): binary parsed... cur_token:" + tlist.get(cur_token).name);

			
			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				//#1
				write_file(tlist.get(cur_token).name);
				
				// #5
				if(is_func_level)
				{
					int param_ind=create_param_entry(tlist.get(cur_token).name);
					//System.out.println("parameter_list(): param ="+tlist.get(cur_token).name+", is created at param ind="+param_ind);
				}
				
				cur_token++;
				////System.out.println("INSIDE parameter_list(): binray ID parsed... cur_token:" + tlist.get(cur_token).name);

				if(non_empty_list_0())
				{
					////System.out.println("<parameter_list> parsed...");
					return true;
				}
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("decimal"))
		{
			//#1
			write_file("decimal ");		
			cur_token++;
			////System.out.println("INSIDE parameter_list(): decimal parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				//#1
				write_file(tlist.get(cur_token).name);
				
				// #5
				if(is_func_level)
				{
					int param_ind=create_param_entry(tlist.get(cur_token).name);
					//System.out.println("parameter_list(): param ="+tlist.get(cur_token).name+", is created at param ind="+param_ind);
				}
				
				cur_token++;
				////System.out.println("INSIDE parameter_list(): decimal ID parsed... cur_token:" + tlist.get(cur_token).name);

				if(non_empty_list_0())
				{
					////System.out.println("<parameter_list> parsed...");
					return true;
				}
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean func_1()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_1(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_1(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals(";"))
		{
			//#1
			write_file(";\n");
			cur_token++;
			////System.out.println("<func_1>  parsed...");

			return true;
		}
		else if(tlist.get(cur_token).name.equals("{"))
		{
			// Definition of a new function...
			// Need to initialize Symbol Table for this function...
			// Set current function name as current id name
			//#1
			write_file("{\n");
			
			is_global=false;
			
			curr_func.setLength(0);
			curr_func.append(curr_id.toString());
			//System.out.println("func_1(): curr_id =" + curr_id.toString() + " , curr_func =" + curr_func.toString());
			
			//SymTable temp_st=new SymTable();
			curr_stable.clear();
			curr_st_index=0;
			//func_tables.put(curr_func.toString(),temp_st);
			//System.out.println("Symbol table initialized for function:"+curr_func.toString());
			//System.out.println("ArrayList Statements initialized for function:"+curr_func.toString());
			curr_func_stmts.clear();
			curr_stmt.setLength(0);
			curr_op.setLength(0);
			
			cur_token++;
			////System.out.println("INSIDE func_1(): { parsed... cur_token:" + tlist.get(cur_token).name);

			if(func_2())
			{
				////System.out.println("<func_1> parsed... FUNc incremented ### 1");
				
				num_func++;
			
				// add int local[loc_st_index] statement and write all statements to file...
				decl_and_print_all();
				
				//#1
				write_file("}\n");
				
				curr_func_stmts.clear();
				is_global=true;
				return true;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean func_4()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_4(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_4(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals(";"))
		{
			//#1
			write_file(";\n");
			cur_token++;
			////System.out.println("<func_4> parsed...");

			return true;
		}
		else if(tlist.get(cur_token).name.equals("{"))
		{
			// Definition of a new function...
			// Need to initialize Symbol Table for this function...
			// Set current function name as current id name
			
			//#1
			write_file("{\n");
			curr_func.setLength(0);
			curr_func.append(curr_id.toString());
			//System.out.println("func_4(): curr_id =" + curr_id.toString() + " , curr_func =" + curr_func.toString());
			
			
			//SymTable temp_st=new SymTable();
			curr_stable.clear();
			curr_st_index=0;
			//func_tables.put(curr_func.toString(),temp_st);
			//System.out.println("Symbol table initialized for function:"+curr_func.toString());
			//System.out.println("ArrayList Statements initialized for function:"+curr_func.toString());
			curr_func_stmts.clear();
			curr_stmt.setLength(0);
			is_global=false;
			
			cur_token++;
			////System.out.println("INSIDE func_4(): { parsed... cur_token:" + tlist.get(cur_token).name);

			if(func_5())
			{
				////System.out.println("<func_4> parsed...FUNc incremented ### 4");
				
				num_func++;
			
				// add int local[loc_st_index] statement and write all statements to file...
				decl_and_print_all();
				
				//#1
				write_file("}\n");
				is_global=true;
				return true;
			}
			
			cur_token--;
			return false;
		}
		
		return false;	
	}
	
	public static boolean func_list()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_list(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_list(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(func())
		{
			////System.out.println("INSIDE func_list(): <func> parsed... ");
				//	"cur_token:"+ tlist.get(cur_token).name);
			//num_func++;
			if(func_list_0())
			{
				////System.out.println("<func_list> parsed...");

				return true;
			}
			return false;
		}
		return false;
	}
	
	
	public static boolean factor(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE factor(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE factor(): parsed... cur_token:" + tlist.get(cur_token).name);

		curr_node_name.setLength(0);
		
		if(tlist.get(cur_token).ttype==Token_type.ID)
		{
			// new ID found in factor()
			// new ID found in read
			curr_id.setLength(0);
			curr_id.append(tlist.get(cur_token).name);
			//System.out.println("factor() : curr_func="+ curr_func.toString() + ", curr_id =" + curr_id.toString());

			cur_token++;
			////System.out.println("INSIDE factor(): ID parsed... cur_token:" + tlist.get(cur_token).name);

			if(factor_0(this_top))
			{
				////System.out.println("<factor> parsed...");
				// create node for ID...
				
				curr_root=create_node(this_top,curr_node_name.toString(),node_type.ID1);
				//System.out.println("Factor()=: Node created = "+ curr_node_name.toString() +", root = " + curr_root.name);
				curr_node_name.setLength(0);
			
				return true;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.NUMBER)
		{
			curr_root=create_node(this_top,tlist.get(cur_token).name,node_type.NUM1);
			curr_node_name.setLength(0);
			cur_token++;
			////System.out.println("<factor> parsed...");
			
			return true;
		}
		else if(tlist.get(cur_token).name.equals("-"))
		{
			cur_token++;
			////System.out.println("INSIDE factor(): - parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).ttype==Token_type.NUMBER)
			{
				curr_root=create_node(this_top,"-"+tlist.get(cur_token).name,node_type.NUM1);
				curr_node_name.setLength(0);
				cur_token++;
				////System.out.println("<factor> : parsed...");

				return true;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("("))
		{
			cur_token++;
			////System.out.println("INSIDE factor(): ( parsed... cur_token:" + tlist.get(cur_token).name);
			
			ASTNode prev_top=this_top;
			curr_root=null;
			curr_expr.setLength(0);
			
			if(expression(curr_root))
			{
				////System.out.println("INSIDE factor(): ( <expresssion> parsed... cur_token:" + tlist.get(cur_token).name);
				
				//System.out.println(" ######### (expr TREE is :::::::: ");
				print_tree(curr_root);
				
				String expr_str=expr_tree_walker(curr_root);
				curr_expr.append(expr_str);
				
				//System.out.println("sub expr string:" + curr_expr);
				curr_root=prev_top;
				this_top=prev_top;
				
				//curr_node
				curr_node_name.append(curr_expr.toString());
				// create node for ID...
				
				curr_root=create_node(this_top,curr_node_name.toString(),node_type.ID1);
				//System.out.println("Factor()=: Node created = "+ curr_node_name.toString() +", root = " + curr_root.name);
				curr_node_name.setLength(0);
				curr_expr.setLength(0);
				
				if(tlist.get(cur_token).name.equals(")"))
				{
					cur_token++;
					////System.out.println("<factor> parsed...");

					return true;
				}
				
				return false;
			}
			
			cur_token--;
			
			return false;
		}
		
		return false;
	}
	
	public static boolean term_0(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE term_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE term_0(): parsed... cur_token:" + tlist.get(cur_token).name);
		
		if(mulop(this_top))
		{
			////System.out.println("INSIDE term_0(): <mulop> parsed... cur_token:" + tlist.get(cur_token).name);
			this_top=get_updated_top(curr_root);
			
			if(factor(this_top))
			{
				////System.out.println("INSIDE term_0(): <mulop> <factor> parsed... cur_token:" + tlist.get(cur_token).name);
				
				this_top=get_updated_top(curr_root);
				
				if(term_0(this_top))
				{
					////System.out.println("<term_0> parsed...");
					
					return true;
				}
				return false;
			}
			return false;
		}
		////System.out.println("<term_0> : Empty parsed...");
		
		return true;
	}
	
	public static boolean expression_0(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE expression_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE expression_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(addop(this_top))
		{
			////System.out.println("INSIDE expression_0(): <addop> parsed... cur_token:" + tlist.get(cur_token).name);
			this_top=get_updated_top(curr_root);
			
			if(term(this_top))
			{
				////System.out.println("INSIDE expression_0(): <addop> <term> parsed... cur_token:" + tlist.get(cur_token).name);
				
				this_top=get_updated_top(curr_root);
				
				if(expression_0(this_top))
				{
					////System.out.println("<expression_0> parsed...");

					return true;
				}
				return false;
			}
			return false;
		}
		
		////System.out.println("<expression_0> : EMpty parsed...");

		return true;
	}
	
	public static boolean func_list_0()
	{
		////System.out.println("INSIDE func_list_0(): parsed... ");
				//"cur_token:" + tlist.get(cur_token).name);

		if(func_list())
		{
			////System.out.println("<func_list_0> parsed...");

			return true;
		}
		
		if(cur_token==num_of_tokens)
		{
			////System.out.println("<func_list_0> : EMpty parsed...");
			return true;
		}
		
		return false;
	}
	
	public static boolean parameter_list_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE parameter_list_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE parameter_list_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).ttype==Token_type.ID)
		{
			//#1
			write_file(tlist.get(cur_token).name);
			cur_token++;
			////System.out.println("INSIDE parameter_list_0(): ID parsed... cur_token:" + tlist.get(cur_token).name);

			if(non_empty_list_0())
			{
				////System.out.println("<parameter_list_0> parsed...");
	
				return true;
			}
			
			cur_token--;
			return false;
		}
		
		////System.out.println("<parameter_list_0> : Empty parsed...");

		return true;
	}
	
	public static boolean non_empty_list_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE non_empty_list_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE non_empty_list_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals(","))
		{
			//#1
			write_file(", ");
			cur_token++;
			////System.out.println("INSIDE non_empty_list_0(): , parsed... cur_token:" + tlist.get(cur_token).name);

			if(type_name())
			{
				////System.out.println("INSIDE non_empty_list_0(): , <type_name> parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).ttype==Token_type.ID)
				{
					//#1
					curr_op.append(tlist.get(cur_token).name);
					write_file(curr_op.toString());
					
					// #5
					if(is_func_level)
					{
						int param_ind=create_param_entry(tlist.get(cur_token).name);
						//System.out.println("parameter_list(): param ="+tlist.get(cur_token).name+", is created at param ind="+param_ind);
					}
					
					cur_token++;
					
					////System.out.println("INSIDE non_empty_list_0(): , <type_name> ID parsed... cur_token:" + tlist.get(cur_token).name);

					if(non_empty_list_0())
					{
						////System.out.println("<non_empty_list_0> parsed...");

						return true;
					}
					
					cur_token--;
					return false;
							
				}
				return false;
			}
			cur_token--;
			return false;
		}
		////System.out.println("<non_empty_list_0> : EMpty parsed...");

		return true;
	}
	
	public static boolean func_2()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_2(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_2(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(data_decls())
		{
			////System.out.println("INSIDE data_decls(): <data_decls> parsed... cur_token:" + tlist.get(cur_token).name);

			if(func_3())
			{
				////System.out.println("<func_2> parsed...");

				return true;
			}
			
			return false;
		}
		else if(statements())
		{
			////System.out.println("INSIDE func_2(): <statements> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals("}"))
			{
				cur_token++;
				////System.out.println("<func_2> : parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals("}"))
		{
			cur_token++;
			////System.out.println("<func_2> :  parsed...");

			return true;
		}
		
		return false;
		
	}
	
	public static boolean func_5()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_5(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_5(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(data_decls())
		{
			////System.out.println("INSIDE func_5(): <data_decls> parsed... cur_token:" + tlist.get(cur_token).name);

			if(func_6())
			{
				////System.out.println("<func_5> : parsed...");

				return true;
			}
			
			return false;
		}
		else if(statements())
		{
			////System.out.println("INSIDE func_5(): <statements> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals("}"))
			{
				cur_token++;
				////System.out.println("<func_5> parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals("}"))
		{
			cur_token++;
			////System.out.println("<func_5> parsed...");
			return true;
		}
		
		return false;	
	}
	
	public static boolean func()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(type_name())
		{
			////System.out.println("INSIDE func(): <type_name> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).ttype==Token_type.ID)
			{
				// New ID found, it can be global ID name or function name...
				curr_id.setLength(0);
				curr_id.append(tlist.get(cur_token).name);
				
				//System.out.println("func(): curr_id =" + curr_id.toString());
				
				//#1
				curr_op.append(tlist.get(cur_token).name);
								
				cur_token++;
				////System.out.println("INSIDE func(): <type_name> ID parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).name.equals("("))
				{
					//#1
					curr_op.append("(");
					write_file(curr_op.toString());
					cur_token++;
					////System.out.println("INSIDE func(): <type_name> ID ( parsed... cur_token:" + tlist.get(cur_token).name);

					if(func_0())
					{
						////System.out.println("<func> : parsed...");

						return true;
					}
					
					cur_token--;
					return false;
				}
				
				cur_token--;
				return false;
			}
			return false;
		}
		
		return false;
	}
	
	public static boolean factor_0(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE factor_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE factor_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("["))
		{
			// array memeber reference in expr...
			// #9
			
			String array_var=null;
			String prev_id=curr_id.toString();
			
			cur_token++;
			////System.out.println("INSIDE factor_0(): [ parsed... cur_token:" + tlist.get(cur_token).name);
			
			ASTNode prev_root=curr_root;
			
			if(expression(null))
			{
				////System.out.println("INSIDE factor_0(): [ <expression> parsed... cur_token:" + tlist.get(cur_token).name);

				//System.out.println(" ########### Factor_0() :  array TREE is :::::::: ");
				print_tree(curr_root);
				
				String expr_str=expr_tree_walker(curr_root);
				//System.out.println("expression for global array size is="+expr_str+ ", prev_id="+prev_id);
				//curr_stmt.append(expr_str);
				
				// #7 - array[2+local[0]]
				// need to add this expr_str to starting index of this array in local or global array...
				// fetch starting index
				
				int start_ind=get_starting_index(prev_id); // -1 if array_name is not recorded in table...
				
				char first_ch=expr_str.charAt(0);
				if(Character.isDigit(first_ch))
				{
					// expr_str is number...
					array_var=String.valueOf(start_ind+Integer.valueOf(expr_str));
				}
				else if(start_ind>0)
				{
					// expr_str is expression but not a number...
					// need to lookup and create node for this expr...
					
					// like array[2+local[0]]
					array_var=start_ind+" + " + expr_str;
				}
				else
				{
					array_var=expr_str;
				}
				
				curr_root=prev_root;
				
				if(tlist.get(cur_token).name.equals("]"))
				{
					// #9
					// array reference in factor...
					// need to assign node name as array_name[expr]...
					
					String stable_name=null;
					
					if(start_ind == -1)
					{
						// todo
						// array is not recored in tables before
						// need to create entry in appropriate stable about this new variable... 
						
						String array_ref_name=prev_id+"["+array_var+"]"; // array[array_var] which is invalid or new
						int temp_ind=lookup_and_create(array_ref_name);	//  new stable entry created as key: array[array_var]
						
						if(!is_global)
						{
							curr_node_name.append("local["+temp_ind+"]");
							String new_st=curr_node_name+" = "+array_ref_name+";";
							curr_func_stmts.add(new_st);
						}
					}
					else
					{
						stable_name=get_array_stable_name(prev_id);
						curr_node_name.append(stable_name+"["+array_var+"]");
					}
					

					//System.out.println("##### factor_0(): ARRAY_REF: ...curr_node_name = " + curr_node_name.toString() + ", curr_expr="+ curr_expr.toString() + " , entry found in stable="+stable_name);
					
					cur_token++;
					////System.out.println("<factor_0> : parsed...");

					return true;
				}
				
				return false;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("("))
		{
			//todo function call in expression...
			// add it in local reference...
			
			//System.out.println("factor_0(): Function call in expr...");
			curr_expr.append(curr_id.toString()+"(");
			String temp_expr=curr_expr.toString();
			
			cur_token++;
			////System.out.println("INSIDE factor_0(): ( parsed... cur_token:" + tlist.get(cur_token).name);

			if(factor_1(this_top))
			{
				////System.out.println("<factor_0> : parsed...");
				
				// function call with void parameter() in expr...
				//
				
				//System.out.println("factor_0(): curr_expr="+curr_expr.toString()+", temp_expr="+temp_expr);
				curr_node_name.append(temp_expr+curr_expr+")");
				String stable_name=get_stable_name(curr_node_name.toString());
				int prev_ind;
				if(stable_name.equals("global"))
				{
					prev_ind=global_st_index;
				}
				else
				{
					prev_ind=curr_st_index;
				}
				int fn_ind=lookup_and_create(curr_node_name.toString());
				
				// #10
				// node already exists...
				if(fn_ind<prev_ind)
				{
					// we need to update this node's key to latest entry in stable...
					
					if(stable_name.equals("global"))
					{
						global_stable.put(curr_node_name.toString(),global_st_index++);
					}
					else
					{
						curr_stable.put(curr_node_name.toString(),curr_st_index++);
					}
					fn_ind=lookup_and_create(curr_node_name.toString());
				}
				
				String func_in_expr=stable_name+"["+fn_ind+"] = "+curr_node_name.toString()+";";
				//System.out.println("##### Function call() in expr...curr_node_name = " + curr_node_name.toString() + ", curr_expr="+ curr_expr.toString());
				
				if(!is_global)
				{
					curr_func_stmts.add(func_in_expr);
				}
				else
				{
					global_stmts.add(func_in_expr);
				}
				curr_expr.setLength(0);
				return true;
			}
			
			cur_token--;
			
			return false;
		}
		
		////System.out.println("<factor_0>: Empty parsed...");
		// lookup and create
		/*
		int loc_ind=lookup_and_create(curr_id.toString());
		curr_node_name.append("local["+loc_ind+"]");
		*/
		curr_node_name.append(curr_id.toString());
		// create node for ID...
		/*
		curr_root=create_node(this_top,curr_node_name.toString(),node_type.ID1);
		//System.out.println("Factor()=: Node created = "+ curr_node_name.toString() +", root = " + curr_root.name);
		curr_node_name.setLength(0);
		*/
		return true;
		
	}
	
	public static boolean mulop(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE mulop(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE mulop(): parsed... cur_token:" + tlist.get(cur_token).name);
		
		if(tlist.get(cur_token).name.equals("*"))
		{
			// create * node...
			curr_root=create_node(this_top,"*",node_type.MULOP1);
			//System.out.println(" * MULOP1 node created...");
			curr_node_name.setLength(0);
			
			cur_token++;
			////System.out.println("<mulop> : parsed...");

			return true;
		}
		else if(tlist.get(cur_token).name.equals("/"))
		{
			// create / node...
			curr_root=create_node(this_top,"/",node_type.MULOP1);
			//System.out.println(" / MULOP1 node created...");
			curr_node_name.setLength(0);
			
			cur_token++;
			////System.out.println("<mulop> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean addop(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE addop(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE addop(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("+"))
		{
			// create + node...
			curr_root=create_node(this_top,"+",node_type.ADDOP1);
			//System.out.println(" + ADDOP1 node created...");
			curr_node_name.setLength(0);
			
			cur_token++;
			////System.out.println("<addop> : parsed...");

			return true;
		}
		else if(tlist.get(cur_token).name.equals("-"))
		{
			// create - node...
			curr_root=create_node(this_top,"-",node_type.ADDOP1);
			//System.out.println(" - ADDOP1 node created...");
			curr_node_name.setLength(0);
			
			cur_token++;
			////System.out.println("<addop> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean term(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE term(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE term(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(factor(this_top))
		{
			////System.out.println("INSIDE term(): <factor> parsed... cur_token:" + tlist.get(cur_token).name);
			
			this_top=get_updated_top(curr_root);
			
			if(term_0(this_top))
			{
				////System.out.println("<term> : parsed...");

				return true;
			}
			
			return false;
		}
		
		return false;
	}
	
	public static boolean data_decls()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE data_decls(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE data_decls(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(type_name())
		{
			////System.out.println("INSIDE data_decls(): <type_name> parsed... cur_token:" + tlist.get(cur_token).name);

			if(id_list())
			{
				////System.out.println("INSIDE data_decls(): <type_name> <id_list> parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).name.equals(";"))
				{
					cur_token++;
					////System.out.println("INSIDE data_decls(): <type_name> <id_list> ; parsed... cur_token:" + tlist.get(cur_token).name);

					if(data_decls_0())
					{
						////System.out.println("<data_decls> : parsed...");

						return true;
					}
					
					cur_token--;
					return false;
				}
				
				return false;
			}
			return false;
		}
		
		return false;
	}
	
	public static boolean func_3()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_3(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_3(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(statements())
		{
			////System.out.println("INSIDE func_3(): <statements> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals("}"))
			{
				cur_token++;
				////System.out.println("<func_3> parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals("}"))
		{
			cur_token++;
			////System.out.println("<func_3> parsed...");

			return true;
		}
		return false;
	}
	
	public static boolean statements()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statements(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE statements(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(statement())
		{
			////System.out.println("INSIDE statements(): <statement> parsed... cur_token:" + tlist.get(cur_token).name);
			
			// todo
			// Add current statment to arraylist...
			
			// #8
			// Control statement like if, else are already added inside statement()...
			if(!is_control_stmt && curr_stmt.length()>0)
			{	
				curr_func_stmts.add(curr_stmt.toString());
			}
			curr_stmt.setLength(0);
			is_control_stmt=false;
			num_stmt++;
			
			if(statements_0())
			{
				////System.out.println("<statements> : parsed...");

				return true;
			}
			
			return false;
		}
		return false;
	}
	
	public static boolean func_6()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE func_6(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE func_6(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(statements())
		{
			////System.out.println("INSIDE func_6():<statements> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals("}"))
			{
				cur_token++;
				////System.out.println("<func_6> : parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals("}"))
		{
			cur_token++;
			////System.out.println("<func_6> : parsed...");
			return true;
		}
		return false;	
	}
	
	public static boolean factor_1(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE factor_1(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE factor_1(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(expr_list(this_top))
		{
			////System.out.println("INSIDE factor_1(): <expr_list> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals(")"))
			{
				cur_token++;
				////System.out.println("<factor_1> parsed...");
				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals(")"))
		{
			cur_token++;
			////System.out.println("<factor_1> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean id_list()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE id_list(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE id_list(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(id())
		{
			//System.out.println("INSIDE id_list(): <id> parsed... cur_token:" + tlist.get(cur_token).name + ", curr op="+curr_op.toString()+" , curr_id="+curr_id.toString());
			
			if(!is_global)
			{
				if(local_array_size<=0)
				{
					int decl_ind=create_local_entry(curr_id.toString());
					//System.out.println("local entry : index= "+ decl_ind+" , id= "+ curr_id.toString());
				}
				else
				{
					//System.out.println("Local array declrearion : array name ="+ curr_id.toString()+", size="+ local_array_size);
					
					local_array_table.put(curr_id.toString(),curr_st_index);
					for(int i=0;i<local_array_size;i++)
					{
						String l_key=curr_id.toString()+"["+i+"]";
						int decl_ind=create_local_entry(l_key);
						//System.out.println("local entry : index= "+ decl_ind+" , id= "+ l_key);
					}
					
					local_array_size=0;
				}
			}
			
			if(id_list_0())
			{
				////System.out.println("<id_list> : parsed...");

				return true;
			}
			return false;
		}
		return false;
	}
	
	public static boolean data_decls_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE data_decls_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE data_decls_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(data_decls())
		{
			////System.out.println("<data_decls_0> : parsed...");

			return true;
		}
		
		////System.out.println("<data_decls_0> : Empty: parsed...");
		
		return true;
	}
	
	public static boolean statement()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statement(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE statement(): parsed... cur_token:" + tlist.get(cur_token).name);
	
		if(tlist.get(cur_token).ttype==Token_type.ID)
		{
			// new ID found in statement... it can be variable assign or array assign...
			curr_id.setLength(0);
			curr_id.append(tlist.get(cur_token).name);
			//System.out.println("statement(): curr_func="+ curr_func.toString() + ", curr_id =" + curr_id.toString());
			
			cur_token++;
			////System.out.println("INSIDE statement(): ID parsed... cur_token:" + tlist.get(cur_token).name);
			
			//curr_stmt.st_type= Stmt_type.ID1;
		
			if(statement_0())
			{
				////System.out.println("<statement> : parsed...");

				return true;
			}
			
			cur_token--;
			
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("if"))
		{
			//is_control_stmt=true;
			cur_token++;
		
			////System.out.println("INSIDE statement(): if parsed... cur_token:" + tlist.get(cur_token).name);
			
			//curr_stmt.st_type= Stmt_type.IF1;
			
			if(tlist.get(cur_token).name.equals("("))
			{
				curr_stmt.append("if(");
				cur_token++;
				////System.out.println("INSIDE statement(): if ( parsed... cur_token:" + tlist.get(cur_token).name);
 
				if(condition_expression())
				{
					////System.out.println("INSIDE statement(): if ( <condition_expression> parsed... cur_token:" + tlist.get(cur_token).name);

					if(tlist.get(cur_token).name.equals(")"))
					{
						// #8
						// handling if with goto
						
						curr_stmt.append(")");
						//curr_func_stmts.add(curr_stmt.toString());
						//curr_stmt.setLength(0);
						
						String c_first="c"+c_cnt+":;";
						String g_first="goto c"+c_cnt+";";
						c_cnt++;
						String c_second="c"+c_cnt+":;";
						String g_second="goto c"+c_cnt+";";
						c_cnt++;
						
						curr_func_stmts.add(curr_stmt.toString() + " " + g_first);
						curr_stmt.setLength(0);
						curr_func_stmts.add(g_second);
						curr_func_stmts.add(c_first);
						cur_token++;
						////System.out.println("INSIDE statement(): if ( <condition_expression> ) parsed... cur_token:" + tlist.get(cur_token).name);

						if(block_statements())
						{
							////System.out.println("<statement> : parsed...");
							
							curr_func_stmts.add(c_second);
							return true;
						}
						cur_token--;
						return false;
					}
					
					return false;
				}
				
				cur_token--;
				return false;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("while"))
		{
			// todo while
			// goto while, need to put goto before condition expression print stmts...
			
			String c_before="c"+c_cnt+":;";
			String g_before="goto c"+c_cnt+";";
				
			// continue will move control to top condition of while loop
			continue_c=c_cnt;
			
			curr_func_stmts.add(c_before);
			c_cnt++;
			
			
			cur_token++;
			////System.out.println("INSIDE statement(): while parsed... cur_token:" + tlist.get(cur_token).name);
			
			//curr_stmt.st_type= Stmt_type.WHILE1;
			
			if(tlist.get(cur_token).name.equals("("))
			{
				// #8
				// change while to if
				curr_stmt.append("if(");
				cur_token++;
				////System.out.println("INSIDE statement(): while ( parsed... cur_token:" + tlist.get(cur_token).name);

				if(condition_expression())
				{
					////System.out.println("INSIDE statement(): while ( < condition_exprssion> parsed... cur_token:" + tlist.get(cur_token).name);

					if(tlist.get(cur_token).name.equals(")"))
					{
						curr_stmt.append(")");
						
						// #8 while
						// handling if with goto
						
						
						//curr_func_stmts.add(curr_stmt.toString());
						//curr_stmt.setLength(0);
						
						
						String c_first="c"+c_cnt+":;";
						String g_first="goto c"+c_cnt+";";
						c_cnt++;
						
						String c_second="c"+c_cnt+":;";
						String g_second="goto c"+c_cnt+";";
						
						// break will move control to end of while loop
						break_c=c_cnt;
						
						c_cnt++;
						
						curr_func_stmts.add(curr_stmt.toString() + " " + g_first);
						curr_stmt.setLength(0);
						curr_func_stmts.add(g_second);
						curr_func_stmts.add(c_first);
						
						cur_token++;
						////System.out.println("INSIDE statement(): while ( < condition_exprssion> ) parsed... cur_token:" + tlist.get(cur_token).name);

						if(block_statements())
						{
							////System.out.println("<statement> : parsed...");
							// #8 while
							
							curr_func_stmts.add(g_before);
							curr_func_stmts.add(c_second);
							return true;
						}
						cur_token--;
						return false;
					}
					
					return false;
				}
				
				cur_token--;
				return false;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("return"))
		{
			curr_stmt.append("return ");
			cur_token++;
			////System.out.println("INSIDE statement(): return parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.RETURN1;
			
			if(statement_2())
			{
				////System.out.println("<statement> : parsed...");
				curr_stmt.append(";");
				return true;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("break"))
		{
			cur_token++;
			////System.out.println("INSIDE statement(): break parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.BREAK1;
			
			if(tlist.get(cur_token).name.equals(";"))
			{
				curr_stmt.setLength(0);
				String break_str="goto c"+break_c+";";
				curr_stmt.append(break_str);
				cur_token++;
				////System.out.println("<statement> : parsed...");

				return true;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("continue"))
		{
			cur_token++;
			////System.out.println("INSIDE statement(): continue parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.CONTINUE1;
			
			if(tlist.get(cur_token).name.equals(";"))
			{
				curr_stmt.setLength(0);
				String continue_str="goto c"+continue_c+";";
				curr_stmt.append(continue_str);
				cur_token++;
				////System.out.println("<statement> : parsed...");

				return true;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("read"))
		{
			cur_token++;
			////System.out.println("INSIDE statement(): read parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.READ1;
			
			if(tlist.get(cur_token).name.equals("("))
			{
				curr_stmt.append("read(");
				cur_token++;
				////System.out.println("INSIDE statement(): read ( parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).ttype==Token_type.ID)
				{
					// new ID found in read
					curr_id.setLength(0);
					curr_id.append(tlist.get(cur_token).name);
					//System.out.println("read : curr_func="+ curr_func.toString() + ", curr_id =" + curr_id.toString());
					

					// lookup and create
					String stable_name=get_stable_name(curr_id.toString());
					int loc_ind=lookup_and_create(curr_id.toString());
					curr_stmt.append(stable_name+"["+loc_ind+"]");
					
					cur_token++;
					////System.out.println("INSIDE statement(): read ( ID parsed... cur_token:" + tlist.get(cur_token).name);

					if(tlist.get(cur_token).name.equals(")"))
					{
						cur_token++;
						////System.out.println("INSIDE statement(): read ( ID ) parsed... cur_token:" + tlist.get(cur_token).name);

						if(tlist.get(cur_token).name.equals(";"))
						{
							curr_stmt.append(");");
							cur_token++;
							////System.out.println("<statement> : parsed...");
							
							return true;
						}
						cur_token--;
						return false;
					}
					cur_token--;
					return false;
				}
				
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("write"))
		{
			cur_token++;
			////System.out.println("INSIDE statement(): write parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.WRITE1;
			
			if(tlist.get(cur_token).name.equals("("))
			{
				curr_stmt.append("write(");
				cur_token++;
				////System.out.println("INSIDE statement(): write ( parsed... cur_token:" + tlist.get(cur_token).name);

				if(expression(curr_root))
				{
					////System.out.println("INSIDE statement(): write ( <expression>  parsed... cur_token:" + tlist.get(cur_token).name);
					
					//System.out.println(" ########################### TREE is :::::::: ");
					print_tree(curr_root);
					
					String expr_str=expr_tree_walker(curr_root);
					//System.out.println("write( : expression returned is =" + expr_str);
					curr_stmt.append(expr_str);
					curr_root=null;
					
					if(tlist.get(cur_token).name.equals(")"))
					{
						cur_token++;
						////System.out.println("INSIDE statement(): write ( <expression> ) parsed... cur_token:" + tlist.get(cur_token).name);

						if(tlist.get(cur_token).name.equals(";"))
						{
							curr_stmt.append(");");
							cur_token++;
							////System.out.println("<statement> : parsed...");

							return true;
						}
						cur_token--;
						return false;
					}
					
					return false;
				}
				
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).ttype==Token_type.RESERVED && tlist.get(cur_token).name.equals("print"))
		{
			cur_token++;
			////System.out.println("INSIDE statement(): print parsed... cur_token:" + tlist.get(cur_token).name);

			//curr_stmt.st_type= Stmt_type.PRINT1;
			
			if(tlist.get(cur_token).name.equals("("))
			{
				curr_stmt.append("print(");
				cur_token++;
				////System.out.println("INSIDE statement(): print ( parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).ttype==Token_type.STRING)
				{
					curr_stmt.append(tlist.get(cur_token).name);
					cur_token++;
					////System.out.println("INSIDE statement(): print ( STRING parsed... cur_token:" + tlist.get(cur_token).name);

					if(tlist.get(cur_token).name.equals(")"))
					{
						cur_token++;
						////System.out.println("INSIDE statement(): print ( STRING ) parsed... cur_token:" + tlist.get(cur_token).name);

						if(tlist.get(cur_token).name.equals(";"))
						{
							curr_stmt.append(");");
							cur_token++;
							////System.out.println("<statement> : parsed...");

							return true;
						}
						cur_token--;
						return false;
					}
					cur_token--;
					return false;
				}
				
				cur_token--;
				return false;
			}
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean statements_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statements_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE statements_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(statements())
		{
			////System.out.println("<statements_0> : parsed...");

			return true;
		}
		////System.out.println("<statements_0> : Empty parsed...");
		//todo::
		return true;
	}
	
	public static boolean expr_list(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE expr_list(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE expr_list(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(non_empty_expr_list(this_top))
		{
			////System.out.println("<expr_list> parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean statement_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statement_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE statement_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("="))
		{
			// variable assign...
			// lookup ID and create if not present in local and global hashmap
			// Add statements...
			String stable_name=get_stable_name(curr_id.toString());
			boolean is_in_local=curr_stable.containsKey(curr_id.toString());
			
			int loc_ind=lookup_and_create(curr_id.toString());
			
			// #5
			// if it is in param_table...then need to add local[]=param statement
			
			if(!is_global && param_table.containsKey(curr_id.toString()) && !is_in_local)
			{
				String id_in_param="local["+loc_ind+"] = "+curr_id.toString()+";";
				
				curr_func_stmts.add(id_in_param);
			}
			
			curr_stmt.append(stable_name+"["+loc_ind+"] = ");
			
			cur_token++;
			
			//System.out.println(" statement_0 : curr_stmt="+curr_stmt.toString()+ " ,next token="+tlist.get(cur_token).name);
			////System.out.println("INSIDE statement_0(): = parsed... cur_token:" + tlist.get(cur_token).name);
			
			
			if(expression(curr_root))
			{
				////System.out.println("INSIDE statement_0(): = <expression> parsed... cur_token:" + tlist.get(cur_token).name);

				//System.out.println(" ######## statement_0 TREE is :::::::: ");
				print_tree(curr_root);
				
				String expr_str=expr_tree_walker(curr_root);
				//System.out.println("statement_0 : after expression : expr_str =" + expr_str);
				curr_stmt.append(expr_str);
				curr_root=null;
				
				if(tlist.get(cur_token).name.equals(";"))
				{
					curr_stmt.append(";");
					cur_token++;
					////System.out.println("<statement_0> : parsed...");

					return true;
				}
				return false;
			}
			
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("["))
		{
			// todo
			//  Statement array-handling...
			
			// #9
			
			String array_var=null;
			String prev_id=curr_id.toString();  // holds array name
			//curr_stmt.append(prev_id+"[");
			String prev_stmt=curr_stmt.toString();
			
			cur_token++;
			////System.out.println("INSIDE statement_0(): [ parsed... cur_token:" + tlist.get(cur_token).name);

			ASTNode prev_root=curr_root;
			
			if(expression(null))
			{
				////System.out.println("INSIDE statement_0(): [ <expression> parsed... cur_token:" + tlist.get(cur_token).name);
				
				//System.out.println(" ########### statement_0() :  array TREE is :::::::: ");
				print_tree(curr_root);
				
				String expr_str=expr_tree_walker(curr_root);
				//System.out.println("expression for global array size is="+expr_str+ ", prev_id="+prev_id + ", prev_stmt="+ prev_stmt.toString());
				//curr_stmt.append(expr_str);
				
				// #7 - array[2+local[0]]
				// need to add this expr_str to starting index of this array in local or global array...
				// fetch starting index
				
				int start_ind=get_starting_index(prev_id); // -1 if array_name is not recorded in table...
				
				char first_ch=expr_str.charAt(0);
				if(start_ind!=-1 && Character.isDigit(first_ch))
				{
					// expr_str is number...
					array_var=String.valueOf(start_ind+Integer.valueOf(expr_str));
				}
				else if(start_ind>0)
				{
					// expr_str is expression but not a number...
					// need to lookup and create node for this expr...
					
					// like array[2+local[0]]
					array_var=start_ind+" + " + expr_str;
				}
				else if(start_ind==0)
				{
					array_var=expr_str;
				}
				
				//System.out.println("statement_0: array_var="+array_var);
				
				curr_root=prev_root;
				
				if(tlist.get(cur_token).name.equals("]"))
				{
					
					// #9
					// need to add array reference in curr_stmt
					// local[array_name] or global[array_name]
					String stable_name=null;
					
					if(start_ind == -1)
					{
						// todo
						// array is not recored in tables before
						// need to create entry in appropriate stable about this new variable... 
						
						String array_ref_name=prev_id+"["+array_var+"]"; // array[array_var] which is invalid or new
						int temp_ind=lookup_and_create(array_ref_name);	//  new stable entry created as key: array[array_var]
						
						if(!is_global)
						{
							curr_stmt.append("local["+temp_ind+"]");
						}
					}
					else
					{
						stable_name=get_array_stable_name(prev_id);
						curr_stmt.append(stable_name+"["+array_var+"]");
					}
					
					//System.out.println("statement_0(): ARRAY_VAR : prev_id="+prev_id + "prev_stmt="+prev_stmt.toString() + ", array_var ="+ array_var);

					
					
					cur_token++;
					////System.out.println("INSIDE statement_0(): [ <expression> ] parsed... cur_token:" + tlist.get(cur_token).name);
					
					
					if(tlist.get(cur_token).name.equals("="))
					{
						curr_stmt.append(" = ");
						
						cur_token++;
						////System.out.println("INSIDE statement_0(): [ <expression> ] = parsed... cur_token:" + tlist.get(cur_token).name);
						
						prev_root=curr_root;
						
						if(expression(null))
						{
							////System.out.println("INSIDE statement_0(): [ <expression> ] = <expression> parsed... cur_token:" + tlist.get(cur_token).name);
							
							//System.out.println(" ######## statement_0 TREE is :::::::: ");
							print_tree(curr_root);
							
							expr_str=expr_tree_walker(curr_root);
							//System.out.println("statement_0 : after expression : expr_str =" + expr_str);
							curr_stmt.append(expr_str);
							
							curr_root=prev_root;
							
							if(tlist.get(cur_token).name.equals(";"))
							{
								curr_stmt.append(";");
								cur_token++;
								////System.out.println("<statement_0> : parsed...");

								return true;
							}
							return false;
						}
						cur_token--;
						return false;
					}
					cur_token--;
					return false;
				}
				return false;
			}
			cur_token--;
			return false;
		}
		else if(tlist.get(cur_token).name.equals("("))
		{
			// curr_id is a function call
			// need to assign symbol for it...
			// Add "func(" to curr_stmt...

			curr_stmt.setLength(0);
			curr_stmt.append(curr_id.toString()+ "(");
			
			cur_token++;
			//System.out.println("INSIDE statement_0(): ( parsed... cur_token:" + tlist.get(cur_token).name + "curr_stmt="+curr_stmt.toString());

			if(statement_1())
			{
				////System.out.println("<statement_0> : parsed...");
		
				return true;
			}
			
			cur_token--;
			return false;
		}
		return false;
	}
	
	public static boolean condition_expression()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE condition_expression(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE condition_expression(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(condition())
		{
			////System.out.println("INSIDE condition_expression(): <condition> parsed... cur_token:" + tlist.get(cur_token).name);

			if(condition_expression_0())
			{
				////System.out.println("<condition_expression> : parsed...");

				return true;
			}
			return false;
		}
		
		////System.out.println("INSIDE condition_expression(): returning false... cur_token:" + tlist.get(cur_token).name);

		return false;
	}
	
	public static boolean block_statements()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE block_statements(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE block_statements(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("{"))
		{
			// #8
			is_control_stmt=false;
			cur_token++;
			////System.out.println("INSIDE block_statements(): { parsed... cur_token:" + tlist.get(cur_token).name);

			if(block_statements_0())
			{
				////System.out.println("<block_statements> : parsed...");

				return true;
			}
			
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean statement_2()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statement_2(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE statement_2(): parsed... cur_token:" + tlist.get(cur_token).name);
		
		if(expression(curr_root))
		{
			////System.out.println("INSIDE statement_2(): <expression> parsed... cur_token:" + tlist.get(cur_token).name);
			//System.out.println(" ########################### TREE is :::::::: ");
			print_tree(curr_root);
			String expr_str=expr_tree_walker(curr_root);
			curr_stmt.append(expr_str);
			curr_root=null;
			
			if(tlist.get(cur_token).name.equals(";"))
			{
				cur_token++;
				////System.out.println("<statement_2> parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals(";"))
		{
			cur_token++;
			////System.out.println("<statement_2> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean non_empty_expr_list(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE non_empty_expr_list(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE non_empty_expr_list(): parsed... cur_token:" + tlist.get(cur_token).name + " , curr_expr=" + curr_expr.toString());

		//ASTNode prev_expr=this_top;
		
		//String temp_expr=curr_expr.toString();
		curr_expr.setLength(0);
		
		if(expression(null))
		{
			////System.out.println("INSIDE non_empty_expr_list(): <expression> parsed... cur_token:" + tlist.get(cur_token).name);
			
			//System.out.println(" ########### non_empty_expr_list TREE is :::::::: ");
			print_tree(curr_root);
			
			////System.out.println("non_empty_expr_list() + expression : curr_expr= "+curr_expr.toString()+", temp_expr="+temp_expr);
			String expr_str=expr_tree_walker(curr_root);
			curr_expr.append(expr_str);
			//System.out.println(" sub expr string = "+ curr_expr.toString());
			
			
			if(non_empty_expr_list_0(this_top))
			{
				////System.out.println("<non_empty_expr_list> : parsed...");

				return true;
			}
			
			return false;
		}
		return false;
	}
	
	public static boolean statement_1()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE statement_1(): EOF arrived: return FALSE:");
			return false;
		}
		
		//System.out.println("INSIDE statement_1(): parsed... cur_token:" + tlist.get(cur_token).name+ " , curr_stmt="+curr_stmt.toString()+", curr_expr="+ curr_expr.toString());

		if(expr_list(curr_root))
		{
			////System.out.println("INSIDE statement_1(): <expr_list> parsed... cur_token:" + tlist.get(cur_token).name);
			
		
			curr_stmt.append(curr_expr);
			//System.out.println("INSIDE statement_1(): curr_stmt="+curr_stmt.toString()+" , curr_expr="+curr_expr.toString());
			curr_root=null;
			
			if(tlist.get(cur_token).name.equals(")"))
			{
				curr_stmt.append(")");
				cur_token++;
				////System.out.println("INSIDE statement_1(): <expr_list> ) parsed... cur_token:" + tlist.get(cur_token).name);

				if(tlist.get(cur_token).name.equals(";"))
				{
					curr_stmt.append(";");
					cur_token++;
					////System.out.println("<statement_1> : parsed...");

					return true;
				}
				
				cur_token--;
				return false;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals(")"))
		{
			// add ")" to curr_stmt
			curr_stmt.append(")");
			
			
			cur_token++;
			////System.out.println("INSIDE statement_1(): ) parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals(";"))
			{
				curr_stmt.append(";");
				
				
				cur_token++;
				////System.out.println("<statement_1> : parsed...");

				return true;
			}
			cur_token--;
			return false;
		}
		
		return false;
	}
	
	public static boolean condition()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE condition(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE condition(): parsed... cur_token:" + tlist.get(cur_token).name);
		
		if(expression(curr_root))
		{
			////System.out.println("INSIDE condition(): <expression> parsed... cur_token:" + tlist.get(cur_token).name);
			
			//System.out.println(" ########## Condition: Expr 1: TREE is :::::::: ");
			print_tree(curr_root);
			
			String expr_str=expr_tree_walker(curr_root);
			curr_stmt.append(expr_str);
			curr_root=null;
			
			if(comparision_op())
			{
				////System.out.println("INSIDE condition(): <expression> <comparision_op> parsed... cur_token:" + tlist.get(cur_token).name);

				if(expression(curr_root))
				{
					////System.out.println("<condition> : parsed...");
					//System.out.println(" ########## Condition: Expr 1: TREE is :::::::: ");
					print_tree(curr_root);
					
					expr_str=expr_tree_walker(curr_root);
					curr_stmt.append(expr_str);
					curr_root=null;
					
					return true;
				}
				return false;
			}
			return false;
		}
		return false;
	}
	
	public static boolean condition_expression_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE condition_expression_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE condition_expression_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(condition_op())
		{
			////System.out.println("INSIDE condition_expression_0(): <condition_op> parsed... cur_token:" + tlist.get(cur_token).name);

			if(condition())
			{
				////System.out.println("<condition_expression_0> : parsed...");

				return true;
			}
			return false;
		}
		////System.out.println("<condition_expression_0> : Empty parsed...");
		return true;
	}
	
	public static boolean block_statements_0()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE block_statements_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE block_statements_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(statements())
		{
			////System.out.println("INSIDE block_statements_0(): <statements> parsed... cur_token:" + tlist.get(cur_token).name);

			if(tlist.get(cur_token).name.equals("}"))
			{
				cur_token++;
				////System.out.println("<block_statements_0> : parsed...");

				return true;
			}
			return false;
		}
		else if(tlist.get(cur_token).name.equals("}"))
		{
			cur_token++;
			////System.out.println("<block_statements_0> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean non_empty_expr_list_0(ASTNode this_top)
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE non_empty_expr_list_0(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE non_empty_expr_list_0(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals(","))
		{
			cur_token++;
			////System.out.println("INSIDE non_empty_expr_list_0(): , parsed... cur_token:" + tlist.get(cur_token).name);
			//System.out.println("INSIDE non_empty_expr_list_0(): parsed... cur_token:" + tlist.get(cur_token).name + " , curr_expr=" + curr_expr.toString());

			ASTNode prev_expr=this_top;
			
			String temp_expr=curr_expr.toString();
			curr_expr.setLength(0);
			
			if(expression(this_top))
			{
				////System.out.println("INSIDE non_empty_expr_list_0(): , <expression> parsed... cur_token:" + tlist.get(cur_token).name);
				
				//System.out.println(" ########### non_empty_expr_list TREE is :::::::: ");
				print_tree(curr_root);
				
				curr_expr.append(temp_expr+", ");
				String expr_str=expr_tree_walker(curr_root);
				curr_expr.append(expr_str);
				//System.out.println(" sub expr string = "+ curr_expr.toString());
				
				if(non_empty_expr_list_0(this_top))
				{
					////System.out.println("<non_empty_expr_list_0> : parsed...");

					return true;
				}
				
				return false;
			}
			cur_token--;
			return false;
		}
		////System.out.println("<non_empty_expr_list_0> : Empty parsed...");
		
		return true;
	}
	
	public static boolean comparision_op()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE comparision_op(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE comparision_op(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("==") || tlist.get(cur_token).name.equals("!=") 
				|| tlist.get(cur_token).name.equals(">")|| tlist.get(cur_token).name.equals(">=")
				|| tlist.get(cur_token).name.equals("<")|| tlist.get(cur_token).name.equals("<="))
		{
			curr_stmt.append(" " + tlist.get(cur_token).name+" ");
			cur_token++;
			////System.out.println("<comparision_op> : parsed...");

			return true;
		}
		
		return false;
	}
	
	public static boolean condition_op()
	{
		if(cur_token == num_of_tokens)
		{
			////System.out.println("INSIDE condition_op(): EOF arrived: return FALSE:");
			return false;
		}
		
		////System.out.println("INSIDE condition_op(): parsed... cur_token:" + tlist.get(cur_token).name);

		if(tlist.get(cur_token).name.equals("&&") || tlist.get(cur_token).name.equals("||"))
		{
			curr_stmt.append(" "+tlist.get(cur_token).name+" ");
			cur_token++;
			////System.out.println("<condition_op> : parsed...");

			return true;
		}
		
		return false;
	}
	
	
	// Starts parsing...
	public static void parse_tokens()
	{
		// set tlist index counter to zero
		cur_token=0;
		
		if(program_start())				// input file follows given LL(1) grammar
		{
			System.out.println("Pass");
			//+num_var+" function "+ num_func+" statement "+num_stmt);
		}
		else
			System.out.println("Error");
	}
	
	public static int generate_code(String file_name)
	{
		try 
		{
			fin= new FileInputStream(file_name);
			//InputStreamReader isr=new InputStreamReader(fin);
			br=new BufferedReader(new InputStreamReader(fin));
			
			
			// Append "_gen" to the input file name...
			
			int dot_i=file_name.indexOf('.');
			StringBuffer out_file=new StringBuffer(file_name.substring(0,dot_i));
			
			//////System.out.println("input_file=="+args[0]+", first part="+out_file);
	
			out_file.append("_gen");
			out_file.append(file_name.substring(dot_i,file_name.length()));
			
			//////System.out.println("file output name:"+out_file);

			fos=new FileOutputStream(out_file.toString());
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			
			String c_line=null;
				//new StringBuffer(br.readLine().trim());

			c_line=br.readLine();
			
			while(c_line!=null)
			{

				c_line=c_line.trim();
				cnt=0;
				len=c_line.length();
				StringBuffer sb=new StringBuffer();
				
				while(cnt<len)
				{
					////System.out.println("cnt= "+ cnt+ " , len= "+ len+" ,c_line = " + c_line);
					sb.setLength(0);
					//sb.setLength(0);
					// Move ahead until all space or tab passed...
					int j=cnt;
					
					if(c_line.charAt(j)==' ' || c_line.charAt(j)=='\t')
					{
						j++;
						while(c_line.charAt(j)==' ' || c_line.charAt(j)=='\t')
						{
							j++;
						}
					}
					
					sb.append(c_line.substring(cnt,j).toString());
					////System.out.println("before get token: j="+j+" ,cnt="+cnt+", sb:"+sb.toString());
					
					Token t = getNextToken(c_line);
					if(t==null)
					{
						//System.out.println("Pass");
						br.close();
						fin.close();
						
						bw.close();
						fos.close();
						return -1;
					}
					
					/*
					if(t.getTokenType()== Token_type.ID && !t.getTokenName().equals("main"))
					{
						sb.append("cs512");
					}
					*/
					
					sb.append(t.getTokenName());
					//sb.append(" ");
					////System.out.println("j="+j+" ,cnt="+cnt+", sb:"+sb.toString());
					bw.write(sb.toString());
				}
				
				sb.setLength(0);
				sb.append("\n");
				bw.write(sb.toString());
				//c_line.setLength(0);
				//c_line.append(br.readLine().trim());
				c_line=br.readLine();
				//c_line=c_line.trim();
			}
			
			br.close();
			fin.close();
			
			bw.close();
			fos.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			////System.out.println("Argument File not found...");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 1;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length!=1)
		{
			System.out.println("Less arguments...Provide input file_name!");
			return;
		}
		
		// invoke Scanner
		
		if(scan_file(args[0])==-1)		// Input file has invalid character, exit with error message...
		{
			System.out.println("Error in Scanner: Invalid token in source file");
			return;
		}
		
		//////System.out.println("File scanned. Number of tokens: " + tlist.size());
		
		num_of_tokens=tlist.size();
		
		// parse file, start parsing tokens 
		parse_tokens();
		
		/*
		if(generate_code(args[0])==-1)
		{
			//System.out.println("Error in Code_generator: Invalid token in source file");
			return;
		}*/
		
		try{
		bw.close();
		fos.close();
		}  catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
