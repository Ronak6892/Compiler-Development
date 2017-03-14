# Compiler-Development

README:

1.a. How to compile and run your code;

 To compile and Run, goto directory path where the file Code_Generator.java resides and type this command 

 javac Code_Generator.java 
 
Above command will generate Code_Generator.class file, now type command.

 java Code_Generator <input_file.c>
 
 
Replace <input_file.c> with proper name of the file which contains the input C program.



1.b The functionality of your code and a brief description of your implementation including the high-level data structure and algorithms.

Functionality overview:

- Code uses previous work of Scanner and Parser. 
- Code-generation functionality is implemented along with parsing mechanism.
- First it will try to print all global data and then it will print the function definition of each function.
- Code maintains separate symbol table for global data and function data. Only one Local symbol table is used for all functions. This symbol table is cleared every time the function is completely parsed.

- For global data, 
	- It will make an entry to global symbol table and will print the global array declaration once all global data is parsed and total number of global variables and array size is calculated.
	
- For function definition it will print the return type and function name with parameter list.
	- It parses through each statements one by one in the function definition and records each statement into List of statements.
	- For data declaration, it will create symbol table entry for each variable and parameters passed to the function.	
	- For every expression encountered during parsing, it will add new statements to convert expression into three address code.
	  All these new statements will be recorded in current function's statement list.
	- When it reaches the ending right brace of function definition,
		- First it will add the new statement of declaration of local array of total size calculated during that function's parsing and three code statements generation. This new statement is added as first statement in the list.
		- Then it will print all the recorded statements for that function to output file.
		


Implementation with Algorithm and Data structure:

- Output file is opened and writer is created.
- Code generation functionality is added all over on top of parsing mechanism. Three address code is generated along with the parsing. So it is an one pass code generator. If invalid character is arrived in scanning or grammar is not appropriate as previously implemented then this program will produce only part of generated code in the output file.
- When meta statements are parsed, they are directly printed into output file.

- Global data:

	- For global variables and array it maintains global symbol table and separate global array table.
	- When new global variable is parsed, entry in global symbol table is made.
	- When new global array is parsed,
		- Entries for all elements of array are entered into global symbol table.
		- Separate entry is made for the array_name into global array table which maps array name to its starting index in global symbol table.
	- When all the global data is parsed completely, it will print the global array declaration statement indicating the size of global array which has been calculated by global symbol table entries.
	- When function declaration is parsed, they are printed as it is on to output file.
	
	- Data Structures used:
	
		- HashMap to store global symbol table which maps variable to index in global array
			- HashMap<String,Integer> global_stable=new HashMap<String,Integer>();
			
		- HashMap to store global array index which maps array name to starting index in global array
			- HashMap<String,Integer> global_array_table=new HashMap<String,Integer>();
		
		- ArrayList to record all global statements if present
			- ArrayList<String> global_stmts=new ArrayList<String>();
	
	
- Function definition

	- First it prints the function name and parameter list as it is to output file.
	- For all parameters passed to this function, it creates entries in param_table.
	- It initializes the statement list, which will record all the statements including meta-statements for three code generation for this function.
	- For all data declaration of variables, it creates the local symbol table entry.
	- For all data declaration of arrays,
		- It creates the local symbol table entry for all the elements of the array
		- It creates the local array table entry for array name with value as a starting index in local symbol table for that array
		
	- Then it parses, statements one by one and records each statement with updated three address code reference into statement list.
	
	- For every expression encountered in any of statements,
		- It will perform three code conversion for that expression and add meta statements to current function's statement list.
		- Expression encountered in expression will also be handled similarly and separate three code conversion will be performed on this nested expression and their meta statements will also be recorded into current statement list.
	
	- When ending right brace of function definition is parsed, 
		- It will add the local array declaration statement as the first statement in the list.
		- Print all statements recorded in the statement list one by one into the output file.
		
		
	- Data Structures Used:
		- local symbol table which maps ID's to the index in local array
			- HashMap<String,Integer> curr_stable=new HashMap<String,Integer>();
		
		- local array table which maps array names to its starting index in local symbol table.
			- HashMap<String,Integer> local_array_table=new HashMap<String,Integer>();
			
		- Param table to record the parameters passed to the function
			- HashMap<String,Integer> param_table=new HashMap<String,Integer>();
			
		- Statement list to store the all generated statements for that function
			- ArrayList<String> curr_func_stmts=new ArrayList<String>();
		
		[ Note:- Only one instance of each of above data structures are maintained. All data structured are cleared when new function definition is stared to parse to remove the all previously recorded entries.]
		
	
- Expression three code conversion

	- Whenever the new expression is encountered as per the parsing mechanism rule,
		- Created separate abstract syntax tree for the expression, following the term and factor precedence.
		- For every nested expression found in expression,
			- Separate abstract syntax tree is created for the nested expression
			- Tree walker algorithm is applied on this nested expression's AST and all meta statements are recorded in the function list.
			- Tree walker algorithm will finally return the symbol table entry reference for this nested expression and this returned local symbol entry reference can be added as one single node into top level expression's AST.
			- After the top-level expression is fully parsed and it's AST is completely prepared, Tree walker algorithm can be applied on this AST and only one symbol table entry will be returned back. This single symbol table entry consists the three code evaluation of whole expression.
			
		
		- Data Structure used:
		
		- ASTNode class which represents the node of each AST. Each node represents one element of expression like ID or operand.
		
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
		
		
Tree Walker Algorithm:


	expr_tree_walker(node)
	
		- If node is OPERAND,
			- t1 <- expr_tree_walker(node.l_node);
			- t2 <- expr_tree_walker(node.r_node);
			- t3 <- next_register();
			- new_statement <- t3 + " = " + t1 + " "+ node.name + " " + t2 + ";";
			- statement_list.add_entry(new_statement)
			
		- If node is ID,
			- t3 <-  get_entry_from_symbol_table(node.name);
			
		return t3;
		

Control flow handling with goto and labels:

	- IF
		- for if statement,
			- added first goto LABEL1 statement right after the if() statement which will be executed when condition is TRUE.
			- Added second goto LABEL2 statement which will be executed when if condition is FALSE.
			- Added LABEL1 statement
			
			- After the block_statements() is parsed.
				- Added LABEL2 statement 
	
	- WHILE
		- for while statement,
			- Added before LABEL0 statement before the while() statement 
			- Changed while(condition) to if(condition)
			- Added first goto LABEL1 statement right after if() statement which will be executed when condition is TRUE.
			- Added second goto LABEL2 statement which will be executed when if condition is FALSE.
			- Added LABEL1 statement
			
			- After the block_statements() is parsed.
				- Added before goto LABEL0 statement
				- Added LABEL2 statement
				
	- BREAK
		- for break statement
			- Added goto LABEL2 statement to move control flow to first enclosing while loop's ending label.

	- CONTINUE
		- for continue statement
			- Added goto LABEL0 statement to move control flow to first enclosing while loop's before label.
			

			
- All data structures implemented in previous projects(Scanner and Parser) are used.
