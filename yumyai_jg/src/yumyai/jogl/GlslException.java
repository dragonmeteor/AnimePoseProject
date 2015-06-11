package yumyai.jogl;

public class GlslException extends RuntimeException {	
	private static final long serialVersionUID = 9174089499226177646L;

	public GlslException(String msg){ 
		super("GLSL Error\n" + msg); 
	} 

	public GlslException(String msg, Throwable t){ 
		super("GLSL Error\n" + msg,t); 
	} 
}
