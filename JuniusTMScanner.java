class FormParameter {
  public String id;
  public String name;
  public String value;
  public String type;
  HttpParameter realParameter;

  public HttpParameter getHttpParameter() {
    if (!realParameter.name().isBlank())
      return realParameter;
    return makeIntoRealParameter();
  }

  public HttpParameter makeIntoRealParameter() {
    realParameter = HttpParameter.bodyParameter(name, value);
    return realParameter;
  }

  public FormParameter (String in_type, String in_name, String in_id, String in_value) {
    realParameter = HttpParameter.bodyParameter("","");
    id = in_id;
    name = in_name;
    value = in_value;
    type = in_type;
  }

  public FormParameter () {}
}

class Form {
  public Form fatherForm;
  public List<Form> childrenForms;
  public String id;
  public String action;
  public String method;
  public String name;
  public int depth;
  HttpRequest realRequest;

  public List<FormParameter> parameterList;

  Pattern formPattern;

  Pattern idPattern;
  Pattern namePattern;
  Pattern actionPattern;
  Pattern methodPattern;
  Pattern inputPattern;

  Pattern inputTypePattern;
  Pattern inputNamePattern;
  Pattern inputIdPattern;
  Pattern inputValuePattern;

  public void computePatterns() {
    formPattern = Pattern.compile("<form\\b[^>]>[\\s\\S]?<\/form>", Pattern.CASE_INSENSITIVE); 

    idPattern = Pattern.compile("<form\\b[^>]\\bid\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE);
    namePattern = Pattern.compile("<form\\b[^>]\\bname\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE);
    actionPattern = Pattern.compile("<form\\b[^>]\\baction\\s=\\s\"([:\\/\\-\\w.])\"", Pattern.CASE_INSENSITIVE); 
    methodPattern = Pattern.compile("<form\\b[^>]\\bmethod\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE); 
    inputPattern = Pattern.compile("<input\\b[^>]>", Pattern.CASE_INSENSITIVE); 

    inputTypePattern = Pattern.compile("<input\\b[^>]\\btype\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE); 
    inputNamePattern = Pattern.compile("<input\\b[^>]\\bname\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE); 
    inputIdPattern = Pattern.compile("<input\\b[^>]\\bid\\s=\\s\"([\\w])\"", Pattern.CASE_INSENSITIVE); 
    inputValuePattern = Pattern.compile("<input\\b[^>]\\bvalue\\s=\\s\"([\\w]*)\"", Pattern.CASE_INSENSITIVE);
  }

  public HttpRequest getHttpRequest() {
    if (realRequest.hasParameters()) //will have to comeup with better way to check it
      return realRequest;
    return makeIntoRealHttpRequest();
  }

  public HttpRequest makeIntoRealHttpRequest() {
    List<HttpParameter> httpParameters = new ArrayList<>();
    for (FormParameter fp : parameterList) {
      httpParameters.add(pp.getHttpParameter());
    }

    realRequest = HttpRequest.httpRequestFromUrl("INSERT THE URL TO TEST" + action).withMethod(method).withAddedParameters(httpParameters);
    return realRequest;
  }

  public Form(String in_id, String in_action, String in_method, String in_name, int in_depth) {
    childrenForms = new ArrayList<>();
    realRequest = HttpRequest.httpRequest();
    parameterList = new ArrayList<>();
    id = in_id;
    action = in_action;
    method = in_method;
    name = in_name;
    depth = in_depth;
  }

  public void makeMapping() {
    getHttpRequest();
    var sent = http.sendRequest(realRequest);

    if (sent.hasResponse()) {
      var resp = sent.response();

      String body = resp.bodyToString();

      Matcher formMatcher = formPattern.matcher(body);

      while (depth < 4 && formMatcher.find()) {
        String form = formMatcher.group();
        Matcher idMatcher = idPattern.matcher(form);
        Matcher nameMatcher = namePattern.matcher(form);
        Matcher actionMatcher = actionPattern.matcher(form);
        Matcher methodMatcher = methodPattern.matcher(form);

        if () {
          Form toSubmit = new Form((idMatcher.find()) ? idMatcher.group(1) : "",
              (actionMatcher.find()) ? actionMatcher.group(1) : "",
              (methodMatcher.find()) ? methodMatcher.group(1): "",
              (nameMatcher.find()) ? nameMatcher.group(1) : "",
              this.depth + 1);

          Matcher inputMatcher = inputPattern.matcher(form);

          while (inputMatcher.find()) {
            String input = inputMatcher.group();
            Matcher inputTypeMatcher = inputTypePattern.matcher(input);
            Matcher inputNameMatcher = inputNamePattern.matcher(input);
            Matcher inputIdMatcher = inputIdPattern.matcher(input);
            Matcher inputValueMatcher = inputValuePattern.matcher(input);

            FormParameter oneParam = new FormParameter((inputTypeMatcher.find()) ? inputTypeMatcher.group(1) : "",
                (inputNameMatcher.find()) ? inputNameMatcher.group(1) : "",
                (inputIdMatcher.find()) ? inputIdMatcher.group(1) : "",
                (inputValueMatcher.find()) ? inputValueMatcher.group(1) : "");

            toSubmit.parameterList.add(oneParam);
          }
          toSubmit.fatherForm = this;
          toSubmit.makeMapping(); //creates a recursion
                                  //will have to solve it later
          childrenForms.add(toSubmit);
        }
      }
    }
  }

  private List<FormParameter> updateParams(String formId) {
    getHttpRequest();
    var sent = http.sendRequest(realRequest);
    List<FormParameter> updatedparams = new ArrayList<>();
    if (sent.hasResponse()) {
      var resp = sent.response();

      String body = resp.bodyToString();

      Matcher formMatcher = formPattern.matcher(body);
      while (formMatcher.find()) {
        String form = formMatcher.group();
        Matcher idMatcher = idPattern.matcher(form);

        if (idMatcher.group(1) == formId) {
          Matcher inputMatcher = inputPattern.matcher(form);

          while (inputMatcher.find()) {
            String input = inputMatcher.group();
            Matcher inputTypeMatcher = inputTypePattern.matcher(input);
            Matcher inputNameMatcher = inputNamePattern.matcher(input);
            Matcher inputIdMatcher = inputIdPattern.matcher(input);
            Matcher inputValueMatcher = inputValuePattern.matcher(input);

            FormParameter updatedParam = new FormParameter((inputTypeMatcher.find()) ? inputTypeMatcher.group(1) : "",
                (inputNameMatcher.find()) ? inputNameMatcher.group(1) : "",
                (inputIdMatcher.find()) ? inputIdMatcher.group(1) : "",
                (inputValueMatcher.find()) ? inputValueMatcher.group(1) : "");

            updatedparams.add(updatedParam);
          }
          return updatedparams;
        }
      }
    }
    return updatedparams;
  }

  public List<FormParameter> commitSetup(String formId) {
    if (fatherForm) {
      parameterList = fatherForm.commitSetup(this.id);
      clearRequest();
    }
    return updateParams(formId);
  }

  public void conductTesting() {
    for (FormParameter fp : parameterList) {
      fp.value = "Test";
      commitSetup(this.id);
      clearRequest();
      getHttpRequest();
      sent = http.sendRequest(realRequest);
    }
  }

  private void clearRequest() {
    realRequest = HttpRequest.httpRequest();
  }

}

Form mainForm = new Form("", "/ENDPOINT", "GET", "", 0);
mainForm.makeMapping();
mainForm.childrenForms.get(0).conductTesting();
