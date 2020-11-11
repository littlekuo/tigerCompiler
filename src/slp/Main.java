package slp;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashSet;

import slp.Slp.Exp;
import slp.Slp.Exp.Eseq;
import slp.Slp.Exp.Id;
import slp.Slp.Exp.Num;
import slp.Slp.Exp.Op;
import slp.Slp.ExpList;
import slp.Slp.Stm;
import util.Bug;
import util.Todo;
import control.Control;




//for Interpreter, refer to tiger book
class Table{
	String id; int value; Table tail;
	
	Table(String i, int v, Table t) {
		id=i; value=v; tail=t;
	}
	
	
	int lookup(String key) {
		if(key == id) {
			return value;
		}
		return tail.lookup(key);
	}
}

class IntAndTable {
	int i; Table t;
	
	IntAndTable(int ii, Table tt) { 
		i=ii; t=tt;
	}
}


public class Main
{
  // ///////////////////////////////////////////
  // maximum number of args

  private int maxArgsExp(Exp.T exp)
  {
    if(exp instanceof Exp.Id) {
    	return 0;
    }else if(exp instanceof Exp.Num) {
    	return 0;
    }else if(exp instanceof Exp.Eseq) {
    	Exp.Eseq ex = (Exp.Eseq) exp;
    	int n1 = maxArgsStm(ex.stm);
    	int n2 = maxArgsExp(ex.exp);
    	return n1 >= n2 ? n1 : n2;
    }else if(exp instanceof Exp.Op) {
    	Exp.Op ex = (Exp.Op) exp;
    	int n1 = maxArgsExp(ex.left);
    	int n2 = maxArgsExp(ex.right);
    	return n1 >= n2 ? n1 : n2;
    }

    return -1;
  }



  private int maxArgsStm(Stm.T stm)
  {
    if (stm instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) stm;
      int n1 = maxArgsStm(s.s1);
      int n2 = maxArgsStm(s.s2);

      return n1 >= n2 ? n1 : n2;
      
    } else if (stm instanceof Stm.Assign) {
      Stm.Assign s = (Stm.Assign) stm;
      
      return maxArgsExp(s.exp);
      
    } else if (stm instanceof Stm.Print) {
    	
    	Stm.Print s = (Stm.Print) stm;
    	ExpList.T exList = s.explist;
    	int sum = 0;
    	while(!(exList instanceof ExpList.Last)){
    		++sum;
    		ExpList.Pair list = (ExpList.Pair) exList;
    		exList = list.list;
    	}
    	++sum;
    	return sum;
    
    } else
      new Bug();
    return 0;
  }

  // ////////////////////////////////////////
  // interpreter

  Table table;

  private IntAndTable interpExp(Exp.T exp, Table t)
  {
    if(exp instanceof Exp.Id) {
        
    	Exp.Id ex = (Exp.Id) exp;
    	IntAndTable res = new IntAndTable(t.lookup(ex.id), t);
    	return res;
 
    }else if(exp instanceof Exp.Num) {
    	
    	Exp.Num ex = (Exp.Num) exp;  
    	IntAndTable res = new IntAndTable(ex.num, t);
    	return res;
    	
    }
    else if(exp instanceof Exp.Op) {

    	Exp.Op ex = (Exp.Op) exp;
    	IntAndTable l = interpExp(ex.left, t);
    	IntAndTable r = interpExp(ex.right, l.t);
    	int v = 0;
  
    	switch(ex.op) {
    	 case ADD:
    		 v = l.i + r.i;
    		 break;
    	 case SUB:
    		 v = l.i - r.i;
    		 break;
    	 case TIMES:
    		 v = l.i * r.i;
    		 break;
    	 case DIVIDE:
    		 v = l.i / r.i;
    		 break;
      }
    	IntAndTable res = new IntAndTable(v, r.t);
    	return res;
    		
    }else {

    	Exp.Eseq ex = (Exp.Eseq) exp;
    	Table l = interpStm(ex.stm, t);
    	IntAndTable r = interpExp(ex.exp, l);
      return r;
      
    }
  }


  private Table interpStm(Stm.T prog, Table t)
  {
	  
    if (prog instanceof Stm.Compound) {
      
      Table l = interpStm(((Stm.Compound) prog).s1, t);
      Table r = interpStm(((Stm.Compound) prog).s2, l);
      return r;
      
    } else if (prog instanceof Stm.Assign) {
      
      Stm.Assign pr = (Stm.Assign) prog;
      IntAndTable r =  interpExp(pr.exp, t);
      //update
      Table res = new Table(pr.id, r.i, r.t);
     
      return res;
    	
    } else {
      
    	Stm.Print pr = (Stm.Print) prog;
    	
      ExpList.T exList = pr.explist;
      int cnt = 0;
    	IntAndTable l ;
    	
    	while(exList instanceof ExpList.Pair) {
        
        ++cnt;
        l = interpExp(((ExpList.Pair) exList).exp, t);
        if(cnt > 1) {
        			System.out.print(" ");
        }
        System.out.print(l.i);
        t = l.t;
        exList = ((ExpList.Pair) exList).list;

    	}
    	
    	++cnt;
      l = interpExp(((ExpList.Last) exList).exp, t);
      if(cnt > 1) {
      System.out.print(" ");
     	}
      System.out.print(l.i);
    	System.out.print("\n");
      return l.t;
      
    } 
    
  }
  // ////////////////////////////////////////
  // compile
  HashSet<String> ids;
  StringBuffer buf;

  private void emit(String s)
  {
    buf.append(s);
  }

  private void compileExp(Exp.T exp)
  {
    if (exp instanceof Id) {
      Exp.Id e = (Exp.Id) exp;
      String id = e.id;

      emit("\tmovl\t" + id + ", %eax\n");
    } else if (exp instanceof Num) {
      Exp.Num e = (Exp.Num) exp;
      int num = e.num;

      emit("\tmovl\t$" + num + ", %eax\n");
    } else if (exp instanceof Op) {
      Exp.Op e = (Exp.Op) exp;
      Exp.T left = e.left;
      Exp.T right = e.right;
      Exp.OP_T op = e.op;

      switch (op) {
      case ADD:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\taddl\t%edx, %eax\n");
        break;
      case SUB:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\tsubl\t%eax, %edx\n");
        emit("\tmovl\t%edx, %eax\n");
        break;
      case TIMES:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\timul\t%edx\n");
        break;
      case DIVIDE:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\tmovl\t%eax, %ecx\n");
        emit("\tmovl\t%edx, %eax\n");
        emit("\tcltd\n");
        emit("\tdiv\t%ecx\n");
        break;
      default:
        new Bug();
      }
    } else if (exp instanceof Eseq) {
      Eseq e = (Eseq) exp;
      Stm.T stm = e.stm;
      Exp.T ee = e.exp;

      compileStm(stm);
      compileExp(ee);
    } else
      new Bug();
  }

  private void compileExpList(ExpList.T explist)
  {
    if (explist instanceof ExpList.Pair) {
      ExpList.Pair pair = (ExpList.Pair) explist;
      Exp.T exp = pair.exp;
      ExpList.T list = pair.list;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
      compileExpList(list);
    } else if (explist instanceof ExpList.Last) {
      ExpList.Last last = (ExpList.Last) explist;
      Exp.T exp = last.exp;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  private void compileStm(Stm.T prog)
  {
    if (prog instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) prog;
      Stm.T s1 = s.s1;
      Stm.T s2 = s.s2;

      compileStm(s1);
      compileStm(s2);
    } else if (prog instanceof Stm.Assign) {
      Stm.Assign s = (Stm.Assign) prog;
      String id = s.id;
      Exp.T exp = s.exp;

      ids.add(id);
      compileExp(exp);
      emit("\tmovl\t%eax, " + id + "\n");
    } else if (prog instanceof Stm.Print) {
      Stm.Print s = (Stm.Print) prog;
      ExpList.T explist = s.explist;

      compileExpList(explist);
      emit("\tpushl\t$newline\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  // ////////////////////////////////////////
  public void doit(Stm.T prog)
  {
    // return the maximum number of arguments
    if (Control.ConSlp.action == Control.ConSlp.T.ARGS) {
      int numArgs = maxArgsStm(prog);
      System.out.println(numArgs);
    }

    // interpret a given program
    if (Control.ConSlp.action == Control.ConSlp.T.INTERP) {
      Table t = new Table(null, 0, null);
      interpStm(prog,t);
    }

    // compile a given SLP program to x86
    if (Control.ConSlp.action == Control.ConSlp.T.COMPILE) {
      ids = new HashSet<String>();
      buf = new StringBuffer();

      compileStm(prog);
      try {
        // FileOutputStream out = new FileOutputStream();
        FileWriter writer = new FileWriter("slp_gen.s");
        writer
            .write("// Automatically generated by the Tiger compiler, do NOT edit.\n\n");
        writer.write("\t.data\n");
        writer.write("slp_format:\n");
        writer.write("\t.string \"%d \"\n");
        writer.write("newline:\n");
        writer.write("\t.string \"\\n\"\n");
        for (String s : this.ids) {
          writer.write(s + ":\n");
          writer.write("\t.int 0\n");
        }
        writer.write("\n\n\t.text\n");
        writer.write("\t.globl main\n");
        writer.write("main:\n");
        writer.write("\tpushl\t%ebp\n");
        writer.write("\tmovl\t%esp, %ebp\n");
        writer.write(buf.toString());
        writer.write("\tleave\n\tret\n\n");
        writer.close();
        Process child = Runtime.getRuntime().exec("gcc slp_gen.s");
        child.waitFor();
        if (!Control.ConSlp.keepasm)
          Runtime.getRuntime().exec("rm -rf slp_gen.s");
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
      // System.out.println(buf.toString());
    }
  }
}
