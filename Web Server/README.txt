Install Virtualenv to create the virtual environment for Python 2.7

pip install the following packets to meet the requirements of the program:
1. Flask
2. Flask-Bootstrap
3. python-dev
4. numpy
5. pandas
6. gunicorn

Use Gunicorn to initiate the server by command: gunicorn app:app -b 0.0.0.0:8000

Be sure to allow the program to listen on port 8000 in UFW and online server port management.