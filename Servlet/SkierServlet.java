@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws 
ServletException, IOException {
    System.out.println("Received GET request at /skiers");

    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");

    PrintWriter out = resp.getWriter();
    out.print("{\"message\":\"GET method is working!\"}");
    out.flush();
}

