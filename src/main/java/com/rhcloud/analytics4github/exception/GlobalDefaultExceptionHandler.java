package com.rhcloud.analytics4github.exception;


import org.slf4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Nazar on 28.12.2016.
 */
@ControllerAdvice
public class GlobalDefaultExceptionHandler  {
    private static Logger LOG = getLogger(GlobalDefaultExceptionHandler.class);


}
