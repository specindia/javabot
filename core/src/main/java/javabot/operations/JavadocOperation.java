package javabot.operations;

import java.util.ArrayList;
import java.util.List;

import com.antwerkz.maven.SPI;
import com.antwerkz.sofia.Sofia;
import javabot.IrcEvent;
import javabot.Message;
import javabot.dao.ApiDao;
import javabot.dao.ClazzDao;
import javabot.javadoc.Api;
import javabot.javadoc.Clazz;
import javabot.javadoc.Field;
import javabot.javadoc.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SPI(BotOperation.class)
public class JavadocOperation extends BotOperation {
  @Autowired
  private ApiDao apiDao;
  @Autowired
  private ClazzDao dao;
  private static final int RESULT_LIMIT = 5;

  @Override
  @Transactional
  public List<Message> handleMessage(final IrcEvent event) {
    final String message = event.getMessage();
    final List<Message> responses = new ArrayList<Message>();
    if (message.toLowerCase().startsWith("javadoc")) {
      String key = message.substring("javadoc".length()).trim();
      if (key.startsWith("-list") || key.isEmpty()) {
        displayApiList(event, responses);
      } else {
        Api api = null;
        if (key.startsWith("-")) {
          if (key.contains(" ")) {
            api = apiDao.find(key.substring(1, key.indexOf(" ")));
            key = key.substring(key.indexOf(" ") + 1).trim();
            buildResponse(event, responses, api, key);
          } else {
            displayApiList(event, responses);
          }
        } else {
          buildResponse(event, responses, api, key);
        }
      }
    }
    return responses;
  }

  private void buildResponse(IrcEvent event, List<Message> responses, Api api, String key) {
    final List<String> urls = handle(api, key);
    if (!urls.isEmpty()) {
      StringBuilder urlMessage = new StringBuilder(event.getSender() + ": ");
      String destination = event.getChannel();
      if (urls.size() > RESULT_LIMIT) {
        responses.add(new Message(event.getChannel(), event, Sofia.tooManyResults(event.getSender())));
        destination = event.getSender().getNick();
      }
      urlMessage = buildResponse(event, responses, urls, urlMessage, destination);
      responses.add(new Message(destination, event, urlMessage.toString()));
    } else if (urls.isEmpty()) {
      responses.add(new Message(event.getChannel(), event, Sofia.noDocumentation(key)));
    }
  }

  private StringBuilder buildResponse(IrcEvent event, List<Message> responses, List<String> urls,
    StringBuilder urlMessage, String destination) {
    for (int index = 0; index < urls.size(); index++) {
      if ((urlMessage + urls.get(index)).length() > 400) {
        responses.add(new Message(destination, event, urlMessage.toString()));
        urlMessage = new StringBuilder();
      }
      urlMessage
        .append(index == 0 ? "" : "; ")
        .append(urls.get(index));
    }
    return urlMessage;
  }

  public List<String> handle(final Api api, final String key) {
    final List<String> urls = new ArrayList<String>();
    final int openIndex = key.indexOf('(');
    if (openIndex == -1) {
      parseFieldOrClassRequest(urls, api, key);
    } else {
      parseMethodRequest(urls, api, key, openIndex);
    }
    return urls;
  }

  private void parseFieldOrClassRequest(final List<String> urls, Api api, final String key) {
    final int finalIndex = key.lastIndexOf('.');
    if (finalIndex == -1) {
      findClasses(api, urls, key);
    } else {
      final String className = key.substring(0, finalIndex);
      final String fieldName = key.substring(finalIndex + 1);
      if (Character.isUpperCase(fieldName.charAt(0)) && !fieldName.toUpperCase().equals(fieldName)) {
        findClasses(api, urls, key);
      } else {
        final List<Field> list = dao.getField(api, className, fieldName);
        for (final Field field : list) {
          urls.add(field.getDisplayUrl(field.toString(), dao));
        }
      }
    }
  }

  private void findClasses(Api api, final List<String> urls, final String key) {
    for (final Clazz clazz : dao.getClass(api, key)) {
      urls.add(clazz.getDisplayUrl(clazz.toString(), dao));
    }
  }

  private void parseMethodRequest(final List<String> urls, Api api, final String key, final int openIndex) {
    final int finalIndex = key.lastIndexOf('.', openIndex);
    final int closeIndex = key.indexOf(')');
    if (closeIndex != -1) {
      String className = key.substring(0, finalIndex);
      final String methodName = key.substring(finalIndex + 1, openIndex);
      final String signatureTypes = key.substring(openIndex + 1, closeIndex);
      for (final Method method : dao.getMethods(api, className, methodName, signatureTypes)) {
        urls.add(method.getDisplayUrl(method.toString(), dao));
      }
    }
  }

  private void displayApiList(final IrcEvent event, final List<Message> responses) {
    final StringBuilder builder = new StringBuilder();
    for (final Api api : apiDao.findAll()) {
      if (builder.length() != 0) {
        builder.append("; ");
      }
      String.format("%s(%s)", builder.append(api.getName()), api.getBaseUrl());
    }
    responses.add(new Message(event.getChannel(), event, event.getSender()
      + ", I know of the following APIs: " + builder));
  }
}