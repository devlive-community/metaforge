package org.devlive.metaforge.service.body;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestionBody
{
    private String locale;
    private String content;
    private String type;
    private String engine;
    private String error;
    private String transType;
    private String from; // if from is chat is currently unable to associate context
    private boolean newChat = true;
    private String model;
}
