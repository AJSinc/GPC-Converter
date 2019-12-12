
public enum VirtualMachineT1Code {
	
	T1_VM_VARS("int8 __io_vals__[37];\r\n" + 
			"uint8 __io_val_events__[37];\r\n" + 
			"uint32 __vm_run_time__ = 0;\r\n" + 
			"uint8 __run_vm__ = 1, ___vm_time___ = 10;"),
	
	T1_VM_MAIN("\r\nmain {\r\n" + 
			"int ___i____i;\r\n" + 
			"if(__run_vm__) {\r\n" + 
			"__run_vm__ = 0;\r\n" + 
			"for(___i____i = 0; ___i____i < 37; ++ ___i____i) {\r\n" + 
			"__io_vals__[___i____i] = get_val(___i____i);\r\n" + 
			"__io_val_events__[___i____i] = 0;\r\n" + 
			"}\r\n" + 
			"#ifdef __LAST_COMBO__\r\n" + 
			"for(___i____i = 0; ___i____i < sizeof(__COMBO_RUN__); ++ ___i____i) {\r\n" + 
			"if(__LAST_COMBO__[(___i____i * 3) + ___i____i]) { \r\n" + 
			"__LAST_COMBO__[(___i____i * 3) + ___i____i] = 0;\r\n" + 
			"__COMBO_RUN__[___i____i] = 1;\r\n" + 
			"}\r\n" + 
			"}\r\n" + 
			"#endif\r\n" + 
			"}\r\n" + 
			"else {\r\n" + 
			"for(___i____i = 0; ___i____i < 37; ++ ___i____i) {\r\n" + 
			"set_val(___i____i,  __io_vals__[___i____i]);\r\n" + 
			"if(event_press(___i____i)) __io_val_events__[___i____i] = 1;\r\n" + 
			"if(event_release(___i____i)) __io_val_events__[___i____i] = 3;\r\n" + 
			"}\r\n" + 
			"if(system_time() >= (__vm_run_time__+ ___vm_time___)) {\r\n" + 
			"__run_vm__ = 1; __vm_run_time__ = system_time();\r\n" + 
			"#ifdef __LAST_COMBO__\r\n" + 
			"for(___i____i = 0; ___i____i < sizeof(__COMBO_RUN__); ++ ___i____i) {\r\n" + 
			"if(__COMBO_RUN__[___i____i]) {\r\n" + 
			"__LAST_COMBO__[(___i____i * 3) + ___i____i] = 1;\r\n" + 
			"__COMBO_RUN__[___i____i] = 0;\r\n" + 
			"}\r\n" + 
			"}\r\n" + 
			"#endif\r\n" + 
			"}\r\n" + 
			"}\r\n" + 
			"}\r\n" + 
			""),
	
	T1_VM_FUNCS("\r\nint vm_tctrl_wait(int num) { \r\n" + 
			"return ((num/___vm_time___)+ ((num%___vm_time___) ? 1 : 0));\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			"bool _event_press(uint8 io) {\r\n" + 
			"return event_press(io) || __io_val_events__[io] == 1;\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			"bool _event_release(uint8 io) {\r\n" + 
			"return event_release(io) || __io_val_events__[io] == 3;\r\n" + 
			"}\r\n" + 
			""),
	
	T1_VM_DEFINES("#define vm_tctrl(time) ___vm_time___ = (((10 + time) < 0) ? 0 : (10 + time))\r\n" + 
			"#define wait(a) wait(vm_tctrl_wait(a));");
	
	private final String text;
	
	VirtualMachineT1Code(final String text) {
        this.text = text;
    }
	
	@Override
    public String toString() {
        return text;
    }
	
}
