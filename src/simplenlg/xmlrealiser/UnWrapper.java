package simplenlg.xmlrealiser;

import java.io.Reader;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import simplenlg.features.ClauseStatus;
import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.Inflection;
import simplenlg.features.InternalFeature;
import simplenlg.features.LexicalFeature;
import simplenlg.features.Person;
import simplenlg.features.Tense;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.DocumentCategory;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.ElementCategory;
import simplenlg.framework.InflectedWordElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.framework.PhraseCategory;
import simplenlg.framework.PhraseElement;
import simplenlg.framework.WordElement;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.AdjPhraseSpec;
import simplenlg.phrasespec.AdvPhraseSpec;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;

// UnWrapper maps from classes generated by xjc from RealizerSchema.xsd to
// SimpleNLG classes. There are classes of the same name in two packages.
// The xjc wrapper classes are in the simplenlg.xmlrealiser.wrapper package,
// and are prefixed with the package name.
// The real simplenlg classes are referenced without package name prefix.
/**
 * @author Christopher Howell Agfa Healthcare Corporation
 * 
 */
public class UnWrapper {

	// Create wrapper objects from xml for a request to realise, or the xml for
	// a recording.
	// Both are elements of NLGSpec.
	public static simplenlg.xmlrealiser.wrapper.NLGSpec getNLGSpec(
			Reader xmlReader) throws XMLRealiserException {
		simplenlg.xmlrealiser.wrapper.NLGSpec wt = null;
		try {
			JAXBContext jc = JAXBContext
					.newInstance(simplenlg.xmlrealiser.wrapper.NLGSpec.class);
			Unmarshaller u = jc.createUnmarshaller();
			Object obj = u.unmarshal(xmlReader);
			if (obj instanceof simplenlg.xmlrealiser.wrapper.NLGSpec) {
				wt = (simplenlg.xmlrealiser.wrapper.NLGSpec) obj;
			}
		}

		catch (Throwable e) {
			throw new XMLRealiserException("XML unmarshal error", e);
		}

		return wt;
	}

	NLGFactory factory = null;

	public UnWrapper(Lexicon lexicon) {
		factory = new NLGFactory(lexicon);
	}

	// Create simplenlg objects from wrapper objects.
	public DocumentElement UnwrapDocumentElement(
			simplenlg.xmlrealiser.wrapper.DocumentElement wt) {
		DocumentElement t = factory.createDocument();

		if (wt.getCat() != null) {
			t.setCategory(Enum.valueOf(DocumentCategory.class, wt.getCat()
					.toString()));
		}
		if (wt.getTitle() != null) {
			t.setTitle(wt.getTitle());
		}

		for (simplenlg.xmlrealiser.wrapper.NLGElement wp : wt.getChild()) {
			NLGElement p = UnwrapNLGElement(wp);
			t.addComponent(p);
		}

		return t;
	}

	public NLGElement UnwrapNLGElement(
			simplenlg.xmlrealiser.wrapper.NLGElement wps) {

		if (wps == null) {
			return null;
		}

		if (wps instanceof simplenlg.xmlrealiser.wrapper.DocumentElement) {
			return (NLGElement) UnwrapDocumentElement((simplenlg.xmlrealiser.wrapper.DocumentElement) wps);
		}

		// Handle coordinate phrase specs first, which will cause recursion.
		NLGElement cp = UnwrapCoordinatePhraseSpec(wps);
		if (cp != null) {
			return cp;
		}

		// Literal text.
		if (wps instanceof simplenlg.xmlrealiser.wrapper.StringElement) {
			simplenlg.xmlrealiser.wrapper.StringElement wp = (simplenlg.xmlrealiser.wrapper.StringElement) wps;
			NLGElement p = factory.createStringElement(wp.getVal());
			return p;
		}

		// WordElements (delegate to UnwrapWordElement) -- useful to have
		// because it is called by unWrapPhraseComponents, and pre/post mods
		// might be WordElements
		if(wps instanceof simplenlg.xmlrealiser.wrapper.WordElement) {
			return UnwrapWordElement((simplenlg.xmlrealiser.wrapper.WordElement) wps);
		}

		// Sentence
		else if (wps instanceof simplenlg.xmlrealiser.wrapper.SPhraseSpec) {
			simplenlg.xmlrealiser.wrapper.SPhraseSpec wp = (simplenlg.xmlrealiser.wrapper.SPhraseSpec) wps;
			SPhraseSpec sp = factory.createClause();
			NLGElement vp = null;

			ArrayList<NLGElement> subjects = new ArrayList<NLGElement>();
			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getSubj()) {
				NLGElement p1 = UnwrapNLGElement(p);
				subjects.add(p1);
			}

			if (subjects.size() > 0) {
				sp.setFeature(InternalFeature.SUBJECTS, subjects);
			}

			if (wp.getVp() != null) {
				vp = UnwrapNLGElement(wp.getVp());
				sp.setVerbPhrase(vp);
			}

			if (wp.getCuePhrase() != null) {
				sp.setFeature(Feature.CUE_PHRASE, UnwrapNLGElement(wp
						.getCuePhrase()));
			}

			if (wp.getCOMPLEMENTISER() != null) {
				sp.setFeature(Feature.COMPLEMENTISER, wp.getCOMPLEMENTISER());
			}

			setSFeatures(wp, sp, vp);

			// Common phrase components.
			UnwrapPhraseComponents(sp, wps);

			return sp;
		}

		// Phrases
		else if (wps instanceof simplenlg.xmlrealiser.wrapper.PhraseElement) {
			simplenlg.xmlrealiser.wrapper.PhraseElement we = (simplenlg.xmlrealiser.wrapper.PhraseElement) wps;
			PhraseElement hp = null;
			simplenlg.xmlrealiser.wrapper.WordElement w = we.getHead();
			NLGElement head = UnwrapWordElement(w);

			// Noun Phrase
			if (wps instanceof simplenlg.xmlrealiser.wrapper.NPPhraseSpec) {
				simplenlg.xmlrealiser.wrapper.NPPhraseSpec wp = (simplenlg.xmlrealiser.wrapper.NPPhraseSpec) wps;

				NPPhraseSpec p = factory.createNounPhrase(head);
				hp = p;

				if (wp.getSpec() != null) {
					// p.setSpecifier(UnwrapWordElement(wp.getSpec()));
					simplenlg.xmlrealiser.wrapper.NLGElement spec = wp
							.getSpec();

					if (spec instanceof simplenlg.xmlrealiser.wrapper.WordElement) {
						WordElement specifier = (WordElement) UnwrapWordElement((simplenlg.xmlrealiser.wrapper.WordElement) spec);

						if (specifier != null) {
							p.setSpecifier(specifier);
						}

					} else {
						p.setSpecifier(UnwrapNLGElement(spec));
					}
				}

				setNPFeatures(wp, p);
			}

			// Adjective Phrase
			else if (wps instanceof simplenlg.xmlrealiser.wrapper.AdjPhraseSpec) {
				simplenlg.xmlrealiser.wrapper.AdjPhraseSpec wp = (simplenlg.xmlrealiser.wrapper.AdjPhraseSpec) wps;
				AdjPhraseSpec p = factory.createAdjectivePhrase(head);
				hp = p;

				p.setFeature(Feature.IS_COMPARATIVE, wp.isISCOMPARATIVE());
				p.setFeature(Feature.IS_SUPERLATIVE, wp.isISSUPERLATIVE());
			}

			// Prepositional Phrase
			else if (wps instanceof simplenlg.xmlrealiser.wrapper.PPPhraseSpec) {
				PPPhraseSpec p = factory.createPrepositionPhrase(head);
				hp = p;
			}

			// Adverb Phrase
			else if (wps instanceof simplenlg.xmlrealiser.wrapper.AdvPhraseSpec) {
				simplenlg.xmlrealiser.wrapper.AdvPhraseSpec wp = (simplenlg.xmlrealiser.wrapper.AdvPhraseSpec) wps;
				AdvPhraseSpec p = factory.createAdverbPhrase();
				p.setHead(head);
				hp = p;
				p.setFeature(Feature.IS_COMPARATIVE, wp.isISCOMPARATIVE());
				p.setFeature(Feature.IS_SUPERLATIVE, wp.isISSUPERLATIVE());
			}

			// Verb Phrase
			else if (wps instanceof simplenlg.xmlrealiser.wrapper.VPPhraseSpec) {
				simplenlg.xmlrealiser.wrapper.VPPhraseSpec wp = (simplenlg.xmlrealiser.wrapper.VPPhraseSpec) wps;
				VPPhraseSpec p = factory.createVerbPhrase(head);
				hp = p;
				setVPFeatures(wp, p);
			}

			// Common phrase components.
			UnwrapPhraseComponents(hp, wps);

			return hp;
		}

		return null;
	}

	public void UnwrapPhraseComponents(PhraseElement hp,
			simplenlg.xmlrealiser.wrapper.NLGElement wps) {

		if (hp != null && wps != null) {
			simplenlg.xmlrealiser.wrapper.PhraseElement wp = (simplenlg.xmlrealiser.wrapper.PhraseElement) wps;

			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getFrontMod()) {
				NLGElement p1 = UnwrapNLGElement(p);

				if (p1 != null) {
					hp.addFrontModifier(p1);
				}
			}

			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getPreMod()) {
				NLGElement p1 = UnwrapNLGElement(p);

				if (p1 != null) {
					hp.addPreModifier(p1);
				}
			}

			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getPostMod()) {
				NLGElement p1 = UnwrapNLGElement(p);

				if (p1 != null) {
					hp.addPostModifier(p1);
				}
			}

			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getCompl()) {
				NLGElement p1 = UnwrapNLGElement(p);

				if (p1 != null) {
					hp.addComplement(p1);
				}
			}
		}
	}

	// Returns null if not a coordinate phrase.
	public NLGElement UnwrapCoordinatePhraseSpec(
			simplenlg.xmlrealiser.wrapper.NLGElement wps) {
		NLGElement ret = null;

		// CoordinatedPhraseElement
		if (wps instanceof simplenlg.xmlrealiser.wrapper.CoordinatedPhraseElement) {
			simplenlg.xmlrealiser.wrapper.CoordinatedPhraseElement wp = (simplenlg.xmlrealiser.wrapper.CoordinatedPhraseElement) wps;
			CoordinatedPhraseElement cp = new CoordinatedPhraseElement();
			ElementCategory cat = UnwrapCategory(wp.getCat());
			if (cat != null && cat instanceof PhraseCategory) {
				cp.setCategory(cat);
			}
			if (wp.getConj() != null) {
				String s = wp.getConj();
				if (s != null) {
					cp.setConjunction(s);
				}
			}
			if (wp.getPERSON() != null) {
				cp.setFeature(Feature.PERSON, wp.getPERSON());
			}

			cp.setFeature(Feature.POSSESSIVE, wp.isPOSSESSIVE());

			for (simplenlg.xmlrealiser.wrapper.NLGElement p : wp.getCoord()) {
				NLGElement p1 = UnwrapNLGElement(p);
				if (p1 != null) {
					cp.addCoordinate(p1);
				}

			}
			ret = cp;
		}

		return ret;
	}

	NLGElement UnwrapWordElement(
			simplenlg.xmlrealiser.wrapper.WordElement wordElement) {
		if (wordElement == null) {
			return null;
		}
		LexicalCategory lexCat = LexicalCategory.ANY;
		ElementCategory cat = UnwrapCategory(wordElement.getCat());

		if (cat != null && cat instanceof LexicalCategory) {
			lexCat = (LexicalCategory) cat;
		}

		// String baseForm = getBaseWord(wordElement);
		String baseForm = wordElement.getBase();
		NLGElement word = null;

		if (baseForm != null) {
			word = factory.createWord(baseForm, lexCat);

			if (word instanceof InflectedWordElement
					&& ((InflectedWordElement) word).getBaseWord()
							.getBaseForm().isEmpty()) {
				word = null; // cch TESTING

			} else if (word instanceof WordElement) {
				WordElement we = (WordElement) word;

				// Inflection
				if (wordElement.getVar() != null) {
					Inflection defaultInflection = Enum.valueOf(
							Inflection.class, wordElement.getVar().toString());
					we.setDefaultInflectionalVariant(defaultInflection);
				}

				// Spelling variant may have been given as base form in xml.
				// If so, use that variant.
				if (!baseForm.matches(we.getBaseForm())) {
					we.setDefaultSpellingVariant(baseForm);
				}
			}
		}

		return word;
	}

	// String getBaseWord(simplenlg.xmlrealiser.wrapper.WordElement lex) {
	// // List<String> c = lex.getContent();
	// // if (c.isEmpty())
	// // return "";
	// // else
	// // return (String) c.get(0);
	// return lex.getBase();
	//
	// }

	ElementCategory UnwrapCategory(Object cat) {
		if (cat == null) {
			return null;
		}
		if (cat.getClass().equals(
				simplenlg.xmlrealiser.wrapper.LexicalCategory.class)) {
			return Enum.valueOf(LexicalCategory.class, cat.toString());
		} else if (cat.getClass().equals(
				simplenlg.xmlrealiser.wrapper.PhraseCategory.class)) {
			return Enum.valueOf(PhraseCategory.class, cat.toString());
		} else if (cat.getClass().equals(
				simplenlg.xmlrealiser.wrapper.DocumentCategory.class)) {
			return Enum.valueOf(DocumentCategory.class, cat.toString());
		} else {
			return null;
		}
	}

	void setNPFeatures(simplenlg.xmlrealiser.wrapper.NPPhraseSpec wp,
			simplenlg.phrasespec.NPPhraseSpec p) {
		if (wp.getNUMBER() != null) {
			// map number feature from wrapper ~NumberAgr to actual NumberAgr
			String numString = wp.getNUMBER().toString();
			simplenlg.features.NumberAgreement simplenlgNum = simplenlg.features.NumberAgreement
					.valueOf(numString);
			// p.setFeature(Feature.NUMBER, wp.getNUMBER());
			p.setFeature(Feature.NUMBER, simplenlgNum);
		}

		if (wp.getPERSON() != null) {
			// map person feature from wrapper Person to actual Person
			String perString = wp.getPERSON().toString();
			simplenlg.features.Person simplenlgPers = simplenlg.features.Person
					.valueOf(perString);
			p.setFeature(Feature.PERSON, simplenlgPers);
		}

		if (wp.getGENDER() != null) {
			// map gender feature from wrapper Gender to actual Gender
			String genString = wp.getGENDER().toString();
			simplenlg.features.Gender simplenlgGen = simplenlg.features.Gender
					.valueOf(genString);
			p.setFeature(LexicalFeature.GENDER, simplenlgGen);
		}

		p.setFeature(Feature.ELIDED, wp.isELIDED());
		p.setFeature(Feature.POSSESSIVE, wp.isPOSSESSIVE());
		p.setFeature(Feature.PRONOMINAL, wp.isPRONOMINAL());

	}

	void setVPFeatures(simplenlg.xmlrealiser.wrapper.VPPhraseSpec wp,
			simplenlg.phrasespec.VPPhraseSpec p) {
		if (wp.getFORM() != null) {
			p.setFeature(Feature.FORM, Enum.valueOf(Form.class, wp.getFORM()
					.toString()));
		}

		if (wp.getPERSON() != null) {
			p.setFeature(Feature.PERSON, wp.getPERSON());
		}

		if (wp.getTENSE() != null) {
			p.setFeature(Feature.TENSE, Enum.valueOf(Tense.class, wp.getTENSE()
					.toString()));
		}				
		
		if(wp.getMODAL() != null) {
			p.setFeature(Feature.MODAL, wp.getMODAL());
		}

		p.setFeature(Feature.NEGATED, wp.isNEGATED());
		p.setFeature(Feature.PASSIVE, wp.isPASSIVE());
		p.setFeature(Feature.PERFECT, wp.isPERFECT());
		p.setFeature(Feature.PROGRESSIVE, wp.isPROGRESSIVE());
	}

	/*
	 * Set the features for a sentence. This method also checks whether any
	 * features have been set on the VP, in which case, they are set if they
	 * haven't been set on the S
	 */
	void setSFeatures(simplenlg.xmlrealiser.wrapper.SPhraseSpec wp,
			simplenlg.phrasespec.SPhraseSpec sp,
			simplenlg.framework.NLGElement vp) {

		if (wp.getCLAUSESTATUS() != null) {
			sp.setFeature(InternalFeature.CLAUSE_STATUS, Enum.valueOf(
					ClauseStatus.class, wp.getCLAUSESTATUS().toString()));
		}

		if (wp.getPERSON() != null) {
			sp.setFeature(Feature.PERSON, Enum.valueOf(Person.class, wp.getPERSON().toString()));
		}
		
		if(wp.getFORM() != null) {
			sp.setFeature(Feature.FORM, Enum.valueOf(Form.class, wp.getFORM().toString()));
		}

		if (wp.getTENSE() != null) {
			sp.setFeature(Feature.TENSE, Enum.valueOf(Tense.class, wp
					.getTENSE().toString()));

		} else if (vp != null && vp.hasFeature(Feature.TENSE)) {
			sp.setFeature(Feature.TENSE, vp.getFeature(Feature.TENSE));
		}
		
		//modal -- set on S or inherited from VP
		if(wp.getMODAL() != null) {
			sp.setFeature(Feature.MODAL, wp.getMODAL());
		} else if(vp != null && vp.hasFeature(Feature.MODAL)) {
			sp.setFeature(Feature.MODAL, vp.getFeature(Feature.MODAL));
		}
		
		//passive: can be set on S or VP
		boolean sPass = wp.isPASSIVE() == null ? false : wp.isPASSIVE();
		boolean vPass = vp == null ? false : vp.getFeatureAsBoolean(Feature.PASSIVE).booleanValue();
		sp.setFeature(Feature.PASSIVE, sPass || vPass);
		
		//progressive: can be set on S or VP
		boolean sProg = wp.isPROGRESSIVE() == null ? false : wp.isPROGRESSIVE();
		boolean vProg = vp == null ? false : vp.getFeatureAsBoolean(Feature.PROGRESSIVE).booleanValue();
		sp.setFeature(Feature.PROGRESSIVE, sProg || vProg);
		
		//negation: can be set on S or VP
		boolean sNeg = wp.isNEGATED() == null ? false : wp.isNEGATED();
		boolean vNeg = vp == null ? false : vp.getFeatureAsBoolean(
				Feature.NEGATED).booleanValue();
		sp.setFeature(Feature.NEGATED, sNeg || vNeg);
	}
}
