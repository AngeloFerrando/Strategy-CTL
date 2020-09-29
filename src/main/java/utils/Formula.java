package utils;

import com.google.common.hash.HashCode;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	@SerializedName("operator")
	@Expose
	private String operator;
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

	public String getOperator() { return operator; }

	public void setSubformula(Formula subformula) {
		this.subformula = subformula;
	}

	public List<String> getTerms() {
		return subformula == null ? terms : Stream.concat(terms.stream(), subformula.getTerms().stream()).collect(Collectors.toList());
	}

	public void setTerms(List<String> terms) {
		this.terms = terms;
	}

	public Formula innermostFormula() {
		if(ltl != null && subformula == null) {
			return this;
		} else return subformula.innermostFormula();
	}

	public void updateInnermostFormula(String atom) {
		if(subformula == null) {
			name = null;
			operator = null;
			terms = new ArrayList<>();
			terms.add(atom);
			ltl = atom;
			return;
		}
		if(subformula.ltl != null && subformula.subformula == null) {
			subformula = null;
			terms.add(atom);
			ltl = this.ltl + " " + this.operator + " " + atom;
			operator = null;
		} else {
			subformula.updateInnermostFormula(atom);
		}
	}

	public String extractLTL(Formula formula, String atom) {
		if(this.equals(formula)) {
			return atom;
		}
		if(subformula != null) {
			String subLTL = subformula.extractLTL(formula, atom);
			return subLTL == null ? null : ltl + " " + operator + " " + subLTL;
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Formula)) {
			return false;
		}
		Formula formula = (Formula) obj;
		return
				Objects.equals(this.name, formula.name) &&
				new HashSet<>(this.terms).equals(new HashSet<>(formula.terms)) &&
				Objects.equals(this.ltl, formula.ltl) &&
				Objects.equals(this.subformula, formula.subformula) &&
				Objects.equals(this.operator, formula.operator);
	}

	@Override
	public int hashCode() {
		return
				(this.name == null ? 0 : this.name.hashCode()) +
				(this.ltl == null ? 0 : this.ltl.hashCode()) +
				this.terms.stream().mapToInt(String::hashCode).sum() +
				(this.subformula == null ? 0 : this.subformula.hashCode()) +
				(this.operator == null ? 0 : this.operator.hashCode());
	}

	@Override
	public Formula clone() {
		Formula formula = new Formula();
		formula.name = name;
		formula.terms = new ArrayList<>(this.terms);
		formula.operator = operator;
		formula.ltl = this.ltl;
		if(subformula != null) {
			formula.subformula = subformula.clone();
		}
		return formula;
	}
}
