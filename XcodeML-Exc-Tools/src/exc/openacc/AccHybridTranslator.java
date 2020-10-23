package exc.openacc;
import exc.block.*;
import exc.object.*;
import xcodeml.util.XmOption;
import java.util.*;

public class AccHybridTranslator implements XobjectDefVisitor {
	// private final ACCglobalDecl _globalDecl;
	// private final AccRewriter _rewrite;
	private final XobjectFile _xobjFile;
	private final String _originFileName;
	private final String _acc_ondevice;
	private boolean is_original_file = false;
	private boolean is_next_marker_original_file = false;
	private String include_file;
	private AccInformation accInfo;

	public AccHybridTranslator(XobjectFile xobjFile, String originFileName, String acc_ondevice) {
		if (!XmOption.isLanguageC()) {
			ACC.fatal("current version only supports C language.");
		}

		// _globalDecl = new ACCglobalDecl(xobjFile);
		// _rewrite = new AccRewriter(_globalDecl);
		_xobjFile = xobjFile;
		_originFileName = originFileName;
		_acc_ondevice = acc_ondevice;
		xobjFile.addHeaderLine("*/");
	}

	// AccTranslator から
	@Override
	public void doDef(XobjectDef def) {

		String fname = def.getName();
		// System.out.println("Func name is " + fname);

		XobjectIterator i = new topdownXobjectIterator(def.getFuncBody());
		for (i.init(); !i.end(); i.next()) {
			Xobject x = i.getXobject();
			// if (x != null && (x.isVariable() || x.isVarAddr()))
			// 	System.out.println("Variable '" + x.getName() + "' is referenced from Function '" + fname + "'");
		}

		// Block fb = Bcons.buildFunctionBlock(def);
		// BlockIterator j = new topdownBlockIterator(fb);
		// for(j.init(); !j.end(); j.next()){
		// fb = j.getBlock();
		// // System.out.println("block: " + fb.

		// BasicBlock bb = fb.getBasicBlock();
		// if(bb == null) continue;
		// for(Statement s = bb.getHead(); s != null; s = s.getNext()){
		// Xobject x = s.getExpr();
		// // System.out.println("statement: " + x.getName());

		// // 式xに対する操作 ...
		// }
		// }

		if (!def.isFuncDef()) {
			Xobject v = def.getDef();

			if (v != null && v.Opcode() == Xcode.LINEMARKER && v.getLineNo() != null) {
				String flags = v.getArg(0).getString();
				String filename = v.getLineNo().fileName();

				if (filename.equals(_originFileName)) {
					is_original_file = true;
					is_next_marker_original_file = true;
					if (flags.contains("2"))
						_xobjFile.addHeaderLine("#include " + include_file);
				} else {
					if (is_next_marker_original_file && flags.contains("1")) {
						is_next_marker_original_file = false;
						include_file = filename;
						if (flags.contains("3")) {
							while (include_file.contains("/")) {
								int index = include_file.indexOf("/");
								include_file = include_file.substring(index + 1);
							}
							include_file = "<" + include_file + ">";
						} else
							include_file = "\"" + include_file + "\"";
					}
					is_original_file = false;
				}
			}

			if (!is_original_file) {
				def.setDef(null);
			}
			return;
		}

		if (!is_original_file) {
			def.setDef(null);
			return;
		}

		FuncDefBlock fd = new FuncDefBlock(def);
		FunctionBlock fb = fd.getBlock();
		String funcName = fb.getName();

		if (funcName.equals("main") && _acc_ondevice.equals("FPGA")) {
			// BlockList body = block.getBody();
			// if (body.getDecls() != null) {
			// BlockList newBody = Bcons.emptyBody(body.getIdentList().copy(),
			// body.getDecls().copy());
			// body.setIdentList(null);
			// body.setDecls(null);
			// // newBody.add(Bcons.PRAGMA(Xcode.ACC_PRAGMA, pragmaBlock.getPragma(),
			// // pragmaBlock.getClauses(), body));
			// block.replace(Bcons.COMPOUND(newBody));
			// }

			// remove main function
			def.setDef(null);
			return;
		}

		// XMPrewriteExpr より
		topdownBlockIterator bIter = new topdownBlockIterator(fb);

		for (bIter.init(); !bIter.end(); bIter.next()) {
			Block block = bIter.getBlock();

			if (block.Opcode() == Xcode.ACC_PRAGMA) {
				PragmaBlock pragmaBlock = ((PragmaBlock) block);
				String directiveName = pragmaBlock.getPragma();

				if (directiveName.equals("ONDEVICE")) {
					Xobject clauses = pragmaBlock.getClauses();
					if (clauses == null) {
						// エラーにしたい
						System.out.println("Not found DEVICE!\nusage: #pragma accomn ondevice( DEVICE )");
						System.exit(1);

						// BlockList newBody = Bcons.emptyBody();
						// rewriteACCClauses(clauses, pragmaBlock, fb, localXMPsymbolTable, newBody);
						// if (!newBody.isEmpty()) {
						// bIter.setBlock(Bcons.COMPOUND(newBody));
						// newBody.add(block);
						// }
					}

System.out.println(directiveName);
					try {
						doPragmaInfoReader(pragmaBlock, directiveName);
					} catch (ACCexception e) {
						ACC.error(block.getLineNo(), e.getMessage());
					}
					List<Block> kernelBody = new ArrayList<Block>();
					kernelBody.add(pragmaBlock);				
					AccKernel accKernel = new AccKernel(null, pragmaBlock, accInfo, kernelBody);
					accKernel.analyze();


					// XMPrewriteExprの rewriteACCClauses() を参考に記述
					bottomupXobjectIterator iter = new bottomupXobjectIterator(clauses);

					for (iter.init(); !iter.end(); iter.next()) {
						Xobject x = iter.getXobject();
						if (x == null)
							continue;

						if (x.Opcode() == Xcode.LIST) {
							if (x.left() == null)
								continue;

							String clauseName = x.left().getName();
							if (!clauseName.equals("GPU") && !clauseName.equals("FPGA")) {
								System.out.println(
										"Current version ONLY Supports GPU or FPGA as DEVICE! : #pragma accomn ondevice( DEVICE )");
								System.exit(1);
							}
							if (clauseName.equals(_acc_ondevice)) {
								BlockList body = pragmaBlock.getBody();
								if (body.getDecls() != null) {
									BlockList newBody = Bcons.emptyBody(body.getIdentList().copy(),
											body.getDecls().copy());
									body.setIdentList(null);
									body.setDecls(null);
									// newBody.add(Bcons.PRAGMA(Xcode.ACC_PRAGMA, pragmaBlock.getPragma(),
									// pragmaBlock.getClauses(), body));
									newBody.add(Bcons.COMPOUND(body));
									pragmaBlock.replace(Bcons.COMPOUND(newBody));
								}

								// Block pareblock = pragmaBlock.getParentBlock();
								// pareblock.remove();

								// ブロックからXobjectに戻す！！
								def.setDef(fb.toXobject());
								return;

							} else {
								// if(!accClause.isDataClause()) continue;

								// remove pragma block
								def.setDef(null);
								return;
							}
						}
					}
				} else if (_acc_ondevice.equals("FPGA")) {
					def.setDef(null);
					return;
				}
			}
		}

		if (_acc_ondevice.equals("FPGA")) {
			def.setDef(null);
		}

		// if (def.isFuncDef()) {
		// FuncDefBlock fd = new FuncDefBlock(def);
		// FunctionBlock fb = fd.getBlock();
		// doFuncDef(fb);
		// fd.finalizeBlock();
		// } else {
		// Xobject x = def.getDef();
		// doNonFuncDef(x);
		// }
	}

	void doPragmaInfoReader(PragmaBlock pb, String directiveName) throws ACCexception {
System.out.println(directiveName);

		ACCpragma directive = ACCpragma.valueOf(directiveName);
		if (!directive.isLocalDirective()) {
			throw new ACCexception(directiveName + " is not local directive");
		}

		Xobject clauseList = pb.getClauses();
		accInfo = new AccInformation(directive, clauseList);

	}

	// private void doFuncDef(FunctionBlock fb){
	// _rewrite.doFuncDef(fb);
	// ACC.exitByError();
	// }

	// private void doNonFuncDef(Xobject x){
	// _rewrite.doNonFuncDef(x);
	// ACC.exitByError();
	// }

	// AccProcessor から
	// private void doFuncDef(FunctionBlock fb) {
	// BlockIterator blockIterator;
	// // if (_isTopdown) {
	// blockIterator = new topdownBlockIterator(fb);
	// // } else {
	// // blockIterator = new bottomupBlockIterator(fb);
	// // }

	// for (blockIterator.init(); !blockIterator.end(); blockIterator.next()) {
	// Block b = blockIterator.getBlock();
	// switch (b.Opcode()) {
	// case ACC_PRAGMA:
	// try {
	// doLocalAccPragma((PragmaBlock) b);
	// } catch (ACCexception e) {
	// ACC.error(b.getLineNo(), e.getMessage());
	// }
	// break;
	// case PRAGMA_LINE:
	// if (_warnUnknownPragma) {
	// ACC.warning(b.getLineNo(), "unknown pragma : " + b);
	// }
	// break;
	// default:
	// }
	// }
	// ACC.exitByError();
	// }

	// // AccProcessor から
	// private void doNonFuncDef(Xobject x) {
	// switch (x.Opcode()) {
	// case ACC_PRAGMA:
	// try {
	// doGlobalAccPragma(x);
	// } catch (ACCexception e) {
	// ACC.error(x.getLineNo(), e.getMessage());
	// }
	// break;
	// case PRAGMA_LINE:
	// if (_warnUnknownPragma) {
	// ACC.warning(x.getLineNo(), "unknown pragma : " + x);
	// }
	// break;
	// default:
	// }
	// ACC.exitByError();
	// }

	// void doLocalAccPragma(PragmaBlock pb) throws ACCexception {
	// doAccPragma(pb);
	// }

	// void doGlobalAccPragma(Xobject def) throws ACCexception {
	// doAccPragma(def);
	// }

	// void doAccPragma(PropObject po) throws ACCexception {
	// Object obj = po.getProp(AccDirective.prop);
	// if (obj == null)
	// return;
	// AccDirective dire = (AccDirective) obj;
	// dire.rewrite();
	// }

	// void rewrite() throws ACCexception {
	// if (isDisabled()) {
	// _pb.replace(Bcons.COMPOUND(_pb.getBody()));
	// return;
	// }

	// // build
	// BlockList beginBody = Bcons.emptyBody();
	// for (Block b : initBlockList)
	// beginBody.add(b);
	// for (Block b : copyinBlockList)
	// beginBody.add(b);
	// BlockList endBody = Bcons.emptyBody();
	// for (Block b : copyoutBlockList)
	// endBody.add(b);
	// for (Block b : finalizeBlockList)
	// endBody.add(b);

	// Block beginBlock = Bcons.COMPOUND(beginBody);
	// Block endBlock = Bcons.COMPOUND(endBody);

	// BlockList kernelsBody = Bcons.emptyBody();
	// for (Block b : _kernelBlocks) {
	// kernelsBody.add(b);
	// }
	// Block kernelsBlock = Bcons.COMPOUND(kernelsBody);

	// BlockList resultBody = Bcons.emptyBody();
	// for (Xobject x : idList) {
	// resultBody.addIdent((Ident) x);
	// }

	// Xobject ifExpr = _info.getIntExpr(ACCpragma.IF);
	// boolean isEnabled = (ifExpr == null || (ifExpr.isIntConstant() &&
	// !ifExpr.isZeroConstant()));
	// if (isEnabled) {
	// resultBody.add(beginBlock);
	// resultBody.add(kernelsBlock);
	// resultBody.add(endBlock);
	// } else {
	// Ident condId = resultBody.declLocalIdent("_ACC_DATA_IF_COND", Xtype.charType,
	// StorageClass.AUTO, ifExpr);
	// resultBody.add(Bcons.IF(condId.Ref(), beginBlock, null));
	// resultBody.add(Bcons.IF(condId.Ref(), kernelsBlock,
	// Bcons.COMPOUND(_pb.getBody())));
	// resultBody.add(Bcons.IF(condId.Ref(), endBlock, null));
	// }

	// _pb.replace(Bcons.COMPOUND(resultBody));
	// }

	public void finish() {
		// ヘッダーを出力する？

		_xobjFile.setProgramAttributes(_originFileName, _xobjFile.getLanguageAttribute(), "AccHybridTranslator", _xobjFile.getCompilerInfo()+" "+_xobjFile.getVersion(), _xobjFile.getTime());
		// ACCgpuDecompiler gpuDecompiler = new ACCgpuDecompiler();
		// gpuDecompiler.decompile(_globalDecl);

	}
}