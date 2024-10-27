package idetector.config;

import lombok.extern.slf4j.Slf4j;
import soot.G;
import soot.options.Options;

import java.io.File;


@Slf4j
public class SootConfiguration {


    public static void initSootOption(){
        String output = String.join(File.separator, System.getProperty("user.dir"), "temp");
        log.debug("Output directory: " + output);
        G.reset();

        Options.v().set_verbose(true);

        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_output_dir(output);
        Options.v().set_output_format(Options.output_format_jimple);

        Options.v().set_whole_program(true);
        Options.v().set_no_writeout_body_releasing(true);


    }
}
