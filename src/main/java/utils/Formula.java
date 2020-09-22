package utils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Formula extends JsonObject implements Cloneable {

	@SerializedName("group")
	@Expose
	private String name;
	@SerializedName("sub-formula")
	@Expose
	private Formula subformula;
	@SerializedName("ltl")
	@Expose
	private String ltl;
	@SerializedName("terms")
	@Expose
	private List<String> terms = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Formula getSubformula() {
		return subformula;
	}

	public String getLTLFormula() { return ltl; }

	public void setSubformula(Formula subformula) {
		this.subformula = subformula;
	}

	public List<String> getTerms() {
		return terms;
	}

	public void setTerms(List<String> terms) {
		this.terms = terms;
	}

	public Formula innermostFormula() {
		if(ltl != null) {
			return this;
		} else return subformula.innermostFormula();
	}

	public void updateInnermostFormula(String atom) {
		if(subformula == null) {
			name = null;
			terms = new ArrayList<>();
			ltl = atom;
			return;
		}
		if(subformula.ltl != null) {
			subformula = null;
			ltl = atom;
		} else {
			subformula.updateInnermostFormula(atom);
		}
	}

	@Override
	public Formula clone() {
		Formula formula = new Formula();
		formula.name = name;
		formula.terms = new ArrayList<>(this.terms);
		if(this.subformula != null) {
			formula.subformula = this.subformula.clone();
		} else {
			formula.ltl = this.ltl;
		}
		return formula;
	}
}
