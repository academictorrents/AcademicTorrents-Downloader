package atdown;
/* 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * Author: Joseph Paul Cohen
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * This class is for graphically exploring objects by calling their 
 * "get" and "is" methods that can't be seen during regular eclipse
 * debugging. This is designed for understanding code by looking at 
 * the methods that they offer. 
 * 
 * Some objects have their values as fields. These fields can be seen 
 * from inside the eclipse debugger.  If a get method looks up it's 
 * value in a database or analyzes files these values can't be seen 
 * during normal debugging.
 * 
 * The downfall of this type of examination is that these get functions
 * can have side effects.  For analyzing API's this isn't important.
 * 
 * @author joecohen
 *
 */
public class PojoExplorer implements TreeModel {

	final String UTILITY_NAME = "PojoExplorer - Joseph Paul Cohen";
	
	public static void main(String[] args) {

		JFrame f = new JFrame();
		
		JProgressBar s = new JProgressBar();
		
		new PojoExplorer(s);
		
		PojoExplorer.pausethread();
	}

	public static void pausethread(){
		
		System.out.println("Thread Paused, Press Enter to continue");
		new Scanner(System.in).next();
		System.out.println("Resuming");
	}
	
	protected JProgressBar progressBar = new JProgressBar();
	private List<TreeModelListener> treeListeners = new ArrayList<TreeModelListener>();
	private Node root = new ObjectNode("Loading...", this);
	
	/**
	 * This is designed to be simple to insert into your code. 
	 * All you do is instantiate a new PojoExplorer with the 
	 * object you want to look at like this:<br><br>
	 * 
	 * <code>
	 * new PojoExplorer(f);
	 * </code>
	 * <br><br>
	 * Most of the time you will want to pause the current 
	 * thread so that the value isn't changed. This class has
	 * a function for that:<br><br>
	 * 
	 * <code>
	 * PojoExplorer.pausethread();
	 * </code>
	 * 
	 * 
	 * 
	 * @param Some Object o
	 */
	public PojoExplorer(Object o){
	
		/// start up GUI
		
		JFrame frame = new JFrame();
		frame.setTitle(UTILITY_NAME);
		frame.setMinimumSize(new Dimension(800, 600));
		//frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		
		
		JTree tree = new JTree(this);
		JScrollPane treePanel = new JScrollPane(tree);
		
		
		//progressbar
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		
		JPanel topPanel = new JPanel(new GridLayout(3,1));
		topPanel.add(progressBar);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(treePanel);
		
		frame.add(mainPanel);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println("Can't set Look and Feel to " +UIManager.getSystemLookAndFeelClassName());
			//System.exit(1);
		}
		

		try{
			progressBar.setValue(70);
			progressBar.setString("Building Tree..");
			root = new ObjectNode(o, this);
			progressBar.setValue(80);
			for (TreeModelListener l : treeListeners)
				l.treeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
			progressBar.setValue(0);
			progressBar.setString("");
		}catch(Exception e){
			root = new ObjectNode(e, this);
			for (TreeModelListener l : treeListeners)
				l.treeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
			progressBar.setString("Error");
			progressBar.setValue(0);
		}
		
		frame.setVisible(true);
		System.out.println("Loaded");
	}
	
	@Override
	public Object getRoot() {
		
		return root;
	}

	@Override
	public Object getChild(Object parent, int index) {
		
		if (parent instanceof Node)
			return ((Node)parent).getChild(index);
		else
			return null;
	}

	@Override
	public int getChildCount(Object parent) {
		
		if (parent instanceof Node)
			return ((Node)parent).getChildCount();
		else
			return 0;
	}
	
	@Override
	public boolean isLeaf(Object node) {
		
		if (node instanceof Node)
			return ((Node)node).getChildCount() == 0;
		else
			return true;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		
		if (parent instanceof Node){
			for(int i = 0;i < ((Node)parent).getChildCount();i++)
				if (child.equals(((Node)parent).getChild(i)))
						return i;
		}
		return -1;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		
		treeListeners.add(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		
		treeListeners.remove(l);
	}
}


interface Node{
	
	public int getChildCount();
	public Node getChild(int index);
}

class ObjectNode implements Node{
	
	Object o;
	List<Object> varList = new ArrayList<Object>();
	PojoExplorer e;
	
	public ObjectNode(Object o, PojoExplorer e){
		
		this.o = o;
		this.e = e;
		e.progressBar.setValue(0);
		
		if (o != null){
			// we check to see if we don't have a primitive or boxed type
			if (!o.getClass().isPrimitive() && !(o instanceof String || o instanceof Integer || o instanceof Double || o instanceof Float || o instanceof Long)){
				
				int numFamily = 0;
				//count familys
				for(Class<? extends Object> temp = o.getClass(); temp != Object.class; temp = temp.getSuperclass())
					numFamily++;
				numFamily = 100/numFamily;
						
				for(Class<? extends Object> temp = o.getClass(); temp != Object.class; temp = temp.getSuperclass()){

				    for (Method m : temp.getDeclaredMethods()){
				    	
				    	if ((m.getName().startsWith("get") || m.getName().startsWith("is")) && 
				    			m.getParameterTypes().length == 0 && 
				    			!Modifier.isStatic(m.getModifiers()) &&
				    			!Modifier.isPrivate(m.getModifiers()) &&
				    			!Modifier.isNative(m.getModifiers()) &&
				    			!Modifier.isProtected(m.getModifiers()) &&
				    			!Modifier.isFinal(m.getModifiers())
				    			)
				    		varList.add(m);
				    }
				    for (Field f : temp.getFields()){
				    		varList.add(f);
				    }
				}
				e.progressBar.setValue(e.progressBar.getValue() + numFamily);
			}
		}
		e.progressBar.setValue(100);
	}

	public String toString(){
		
		if (o != null)
			return this.o.getClass().getSimpleName() + " - " + this.o.toString();
		else
			return "null";
	}

	@Override
	public int getChildCount() {
		
		return varList.size();
	}

	@Override
	public Node getChild(int index) {
		
		if (varList.get(index) instanceof Method)
			return new MethodNode((Method)varList.get(index), o, this.e);
		else if (varList.get(index) instanceof Field)
			return new FieldNode((Field)varList.get(index), o, this.e);
		else
			return new ObjectNode(varList.get(index), this.e);
			
	}
	
	public Boolean containsString(String term){
		
		for (int i = 0 ; i < this.getChildCount() ; i++){
			Node target = this.getChild(i);
			if(target != null && target.toString().toLowerCase().contains(term.toLowerCase()))
				return true;
		}
		return false;

	}
	
}

class MethodNode implements Node{
	
	Node node;
	Method m;
	PojoExplorer e;
	
	public MethodNode(Method m, Object o, PojoExplorer e){
		this.m = m;
		this.e = e;
		try {
			Object obj = m.invoke(o, new Object[0]);
			if (obj instanceof Collection)
				node = new CollectionNode((Collection<?>)obj, this.e);
			else if (obj.getClass().isArray())
				node = new ArrayNode((Object[]) obj, this.e);
			else
				node = new ObjectNode(obj, this.e);
		} catch (Throwable ex) {
			//ex.printStackTrace();
			node = new ObjectNode("Error <" + ex.getMessage() + ">", this.e);
		}
	}
	
	public String toString(){
		return m.getName() + " - " + node;
	}

	@Override
	public int getChildCount() {
		return node.getChildCount();
	}

	@Override
	public Node getChild(int index) {
		return node.getChild(index);
	}
}

class FieldNode implements Node{
	
	Node node;
	Field f;
	PojoExplorer e;
	
	public FieldNode(Field f, Object o, PojoExplorer e){
		this.f = f;
		this.e = e;
		try {
			Object obj = f.get(o);
			if (obj instanceof Collection)
				node = new CollectionNode((Collection<?>)obj, this.e);
			else
				node = new ObjectNode(obj, this.e);
		} catch (Throwable ex) {
			//ex.printStackTrace();
			node = new ObjectNode("Error <" + ex.getMessage() + ">", this.e);
		}
	}
	
	public String toString(){
		return f.getName() + " - " + node;
	}

	@Override
	public int getChildCount() {
		return node.getChildCount();
	}

	@Override
	public Node getChild(int index) {
		return node.getChild(index);
	}
}

class CollectionNode implements Node{
		
	Collection<?> c;
	Object[] o  = {};
	PojoExplorer e;
	
	public CollectionNode(Collection<?> c, PojoExplorer e){
		
		this.e = e;
		if (c != null)
			this.o = c.toArray();
	}

	public String toString(){
		if (c != null)
			return this.c.getClass().getSimpleName();
		else
			return "Collection";
	}

	@Override
	public int getChildCount() {
		return o.length;
	}

	@Override
	public Node getChild(int index) {
		if (o[index] instanceof Collection)
			return new CollectionNode((Collection<?>)o[index], this.e);
		else
			return new ObjectNode(o[index], this.e);
	}
}
	
class ArrayNode implements Node{
	
	Collection<?> c;
	Object[] o  = {};
	PojoExplorer e;
	
	public ArrayNode(Object[] o, PojoExplorer e){
		
		this.e = e;
		this.o = o;
	}

	public String toString(){
		if (c != null)
			return this.c.getClass().getSimpleName() + "[]";
		else
			return "Array";
	}

	@Override
	public int getChildCount() {
		return o.length;
	}

	@Override
	public Node getChild(int index) {
		if (o[index] instanceof Collection)
			return new CollectionNode((Collection<?>)o[index], this.e);
		else if (o[index].getClass().isArray())
			return new ArrayNode(o, this.e);
		else
			return new ObjectNode(o[index], this.e);
	}
}