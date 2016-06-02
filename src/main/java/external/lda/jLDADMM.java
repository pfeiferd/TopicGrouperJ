package external.lda;

import java.util.Arrays;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import external.lda.eval.ClusteringEval;
import external.lda.models.GibbsSamplingDMM;
import external.lda.models.GibbsSamplingLDA;
import external.lda.utility.CmdArgs;

/**
 * jLDADMM: A Java package for the LDA and DMM topic models
 * 
 * http://jldadmm.sourceforge.net/
 * 
 * @author: Dat Quoc Nguyen
 * 
 */
public class jLDADMM {
	public static void main(String[] args) {
		CmdArgs cmdArgs = new CmdArgs();
		CmdLineParser parser = new CmdLineParser(cmdArgs);
		try {

			parser.parseArgument(args);

			if (cmdArgs.model.equals("LDA")) {
				double[] alpha = new double[cmdArgs.ntopics];
				Arrays.fill(alpha, cmdArgs.alpha);
				GibbsSamplingLDA lda = new GibbsSamplingLDA(cmdArgs.corpus,
						alpha, cmdArgs.beta, cmdArgs.niters, cmdArgs.twords,
						cmdArgs.expModelName, cmdArgs.initTopicAssgns,
						cmdArgs.savestep);
				lda.inference();
			} else if (cmdArgs.model.equals("DMM")) {
				GibbsSamplingDMM dmm = new GibbsSamplingDMM(cmdArgs.corpus,
						cmdArgs.ntopics, cmdArgs.alpha, cmdArgs.beta,
						cmdArgs.niters, cmdArgs.twords, cmdArgs.expModelName,
						cmdArgs.initTopicAssgns, cmdArgs.savestep);
				dmm.inference();
			} else if (cmdArgs.model.equals("Eval")) {
				ClusteringEval.evaluate(cmdArgs.labelFile, cmdArgs.dir,
						cmdArgs.prob);
			} else {
				System.out
						.println("Error: Option \"-model\" must get \"LDA\" or \"DMM\" or \"Eval\"");
				System.out
						.println("\tLDA: Specify the Latent Dirichlet Allocation topic model");
				System.out
						.println("\tDMM: Specify the one-topic-per-document Dirichlet Multinomial Mixture model");
				System.out
						.println("\tEval: Specify the document clustering evaluation");
				help(parser);
				return;
			}
		} catch (CmdLineException cle) {
			System.out.println("Error: " + cle.getMessage());
			help(parser);
			return;
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public static void help(CmdLineParser parser) {
		System.out
				.println("java -jar jLDADMM.jar [options ...] [arguments...]");
		parser.printUsage(System.out);
	}
}
