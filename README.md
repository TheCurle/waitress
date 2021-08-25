# Waitress

Waitress is a Java-only web server designed for use with Maven repositories.

It serves Maven repos, hence the name.

##Overview
Waitress essentially works with a single lightweight web server, and some backend services.  

When a GET request is made for a file, the following process is run.

* First, the request structure (the part after the site domain) is analysed to look for a maven coordinate.  
If one is found, then:
    * The maven coordinate is checked against an internal list. If the coordinate is hosted on this server, then:
        * Access is checked. If the user (or anonymous) has access to read this file / repository, then it is returned
            via octet-stream.  
          If the user has no access to this file, then a 403 is returned.
    * If the coordinate is hosted on a proxied repository, then a redirect occurs to that repository.
    * If the coordinate is not hosted on any tracked repository, then a 404 is returned.
* If there is no valid maven coordinate in the URL, then:
    * Configured locations are checked - for the user dashboard and admin dashboard.  
      If there is no match, then a 404 is returned.  
      Otherwise, the requested special page is served.
      
There are some edge cases here, such as when a coordinate is present on both the current repository and a proxied.  
In this case and many others, the current repository is prioritised.

Waitress also accepts PUT requests, to upload files.  
Here is the process for a PUT request.

* First, the request structure (the part after the site domain) is analysed to look for a maven coordinate.  
  If one is found, then:
    * Access is checked. If the given user has no access to write to this project, then a 403 is returned.  
      Otherwise, the file is retrieved and emplaced in the given location.
* If there is no valid maven coordinate, then a 404 is returned.


### Authentication
Waitress accepts two kinds of user authentication.

The first kind is BASIC authentication. A username and a password. Compatible with most, if not all browsers currently
available.

The second kind, and last to be implemented, is OAUTH authentication via GitHub. This is to make group management easier.

### Group Management

Users can belong to groups, which themselves belong to Organizations.  
Most Maven servers contain many releases of many projects created by many authors, who write for many teams in many
organizations.

Waitress follows a similar scheme, but with far fewer levels of indirection.

This separation of user-group-org allows for the bulk application of permissions to certain folders for a large amount 
of users, while retaining the capability to audit individual users and grant them more or less permission as required.

### Configuration

Configuration of Waitress is bootstrapped through a TOML file. First, the data directory (where projects that are served
and uploaded, are stored on disk).  

You can also set the username and hash of the password for the owner account. The 
owner account is never denied access to any file, and always has access to the admin endpoint.

The hash for the owner account's password can be generated by putting the password in a text file on disk, and passing
it as a parameter via`-hash-password=FILE_LOCATION`. This will replace the password in the file with the hash.

This process is designed so that the username or password never appear in the shell history.

Once the configuration is bootstrapped, access to the web administration page is granted, and further accounts can be 
created.

### Administration

Administration of Waitress is done through the admin endpoint. The endpoint is accessed through a configurable location.

By navigating to this address in a web browser, the necessary changes can be made to the server and its internal
configuration.

Currently, the admin endpoint allows you to configure:
* Users
* Groups
* Organizations
* Permissions of the above three categories
* Projects, and their owners.
* Proxied repositories (repositories that Waitress will redirect to when given a valid coordinate.)
* The location of the admin endpoint.