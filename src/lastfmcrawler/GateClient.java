package lastfmcrawler;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter: @m1ci
 * www: http://dojchinovski.mk
 */
public class GateClient 
{    
    // corpus pipeline
    private static SerialAnalyserController annotationPipeline = null;
    
    // whether the GATE is initialised
    private static boolean isGateInitilised = false;
    
    public void run(String text){
        
        if(!isGateInitilised){
            
            // initialise GATE
            initialiseGate();            
        }        

        try {                
            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            // create an instance of a English Tokeniser processing resource
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");
        
            // create an instance of a Sentence Splitter processing resource
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
            
            ProcessingResource annieGazetteer = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
            
            // locate the JAPE grammar file
            //File japeOrigFile = new File("C:/Users/Indy/jape-example.jape");
            File japeOrigFile = new File("C:/Users/Indy/artist.jape");
            java.net.URI japeURI = japeOrigFile.toURI();
            
            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                // set the grammar location
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                // set the grammar encoding
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }
            
            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(tokenizerPR);
            annotationPipeline.add(sentenceSplitterPR);
            annotationPipeline.add(annieGazetteer);
            annotationPipeline.add(japeTransducerPR);
            
            // create a document
            Document document = Factory.newDocument(text);

            // create a corpus and add the document
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);

            // set the corpus to the pipeline
            annotationPipeline.setCorpus(corpus);

            //run the pipeline
            annotationPipeline.execute();

            // loop through the documents in the corpus
            for(int i=0; i< corpus.size(); i++){

                Document doc = corpus.get(i);

                // get the default annotation set
                AnnotationSet as_default = doc.getAnnotations();

                FeatureMap futureMap = null;

                // get all Token annotations
                AnnotationSet annSetArtists = as_default.get("Artist",futureMap);
                System.out.println("Number of Artist annotations: " + annSetArtists.size());

                ArrayList artistAnnotations = new ArrayList(annSetArtists);                
                
                System.out.println("Artists: ");
                
                for(int j = 0; j < artistAnnotations.size(); ++j) {
                    // get a token annotation
                    Annotation token = (Annotation)artistAnnotations.get(j);

                    // get the underlying string for the Token
                    Node isaStart = token.getStartNode();
                    Node isaEnd = token.getEndNode();
                    String underlyingString = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    System.out.println(underlyingString);
                }               
            }
        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initialiseGate() {
        
        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files (x86)\\GATE_Developer_7.1");
            Gate.setGateHome(gateHomeFile);
            
            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins            
            File pluginsHome = new File("C:\\Program Files (x86)\\GATE_Developer_7.1/plugins");
            Gate.setPluginsHome(pluginsHome);            
            
            // set user config file (optional)
            // Eg. /Applications/GATE_Developer_7.0/user.xml
            Gate.setUserConfigFile(new File("C:\\Program Files (x86)\\GATE_Developer_7.1", "user.xml"));            
            
            // initialise the GATE library
            Gate.init();
            
            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);
            
            // flag that GATE was successfuly initialised
            isGateInitilised = true;
            
        } catch (MalformedURLException | GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}