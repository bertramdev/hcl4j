package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class SubTypePrimitiveType extends PrimitiveType {

	/**
	 * This field is no longer used as it was silly to not just use the existing Child Tree. as of 0.6.0 this is no
	 * longer used. please reference the getChildren() method for child types if exists.
	 * @deprecated
	 */
	protected PrimitiveType subType;
	
	public SubTypePrimitiveType(PrimitiveType subType,String name, Integer line, Integer column,Integer position) {
		super(name,line,column,position);
		this.subType = subType;
	}

	public PrimitiveType getSubType() {
		if(this.getChildren().size() > 0) {
			if(this.getChildren().get(0) instanceof SubTypePrimitiveType) {
				return (SubTypePrimitiveType)(this.getChildren().get(0));
			}
		}
		return null;
	}

	public String getSymbolName() {
		return "SubTypePrimitiveType";
	}
}
