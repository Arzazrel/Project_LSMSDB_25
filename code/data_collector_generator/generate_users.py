"""
Author: Alessandro Diana
Description:
    
"""

import csv
import bcrypt
import secrets
import string
import random

from faker import Faker

province_codes = [
    "AL", "AT", "BI", "CN", "NO", "TO", "VB", "VC",                         # Piemonte
    "AO",                                                                   # Valle d'Aosta
    "BG", "BS", "CO", "CR", "LC", "LO", "MN", "MI", "MB", "PV", "SO", "VA", # Lombardia
    "BZ", "TN",                                                             # Trentino-Alto Adige
    "BL", "PD", "RO", "TV", "VE", "VR", "VI",                               # Veneto
    "GO", "PN", "TS", "UD",                                                 # Friuli-Venezia Giulia
    "GE", "IM", "SP", "SV",                                                 # Liguria
    "BO", "FC", "FE", "MO", "PR", "PC", "RA", "RE", "RN",                   # Emilia-Romagna
    "AR", "FI", "GR", "LI", "LU", "MS", "PI", "PT", "PO", "SI",             # Toscana
    "PG", "TR",                                                             # Umbria
    "AN", "AP", "FM", "MC", "PU",                                           # Marche
    "FR", "LT", "RI", "RM", "VT",                                           # Lazio
    "AQ", "CH", "PE", "TE",                                                 # Abruzzo
    "CB", "IS",                                                             # Molise
    "AV", "BN", "CE", "NA", "SA",                                           # Campania
    "BA", "BT", "BR", "FG", "LE", "TA",                                     # Puglia
    "MT", "PZ",                                                             # Basilicata
    "CZ", "CS", "KR", "RC", "VV",                                           # Calabria
    "AG", "CL", "CT", "EN", "ME", "PA", "RG", "SR", "TP",                   # Sicilia
    "CA", "NU", "OR", "SS", "SU"                                            # Sardegna
]

# ------------------------------------ start: methods ------------------------------------

# function to generate a random password for the users.
def generate_password(length=12):
    alphabet = string.ascii_letters + string.digits + string.punctuation
    return ''.join(secrets.choice(alphabet) for _ in range(length))

# method to generate the users. Default for our case, 2000 normal users
def generate_users(n=2000, output_file="generated_users.csv"):
    fake = Faker("it_IT")               # Italian localization
    emails = set()

    with open(output_file, mode="w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)

        # Header CSV
        writer.writerow([
            "id",
            "first_name",
            "last_name",
            "email",
            "password_hash",
            "role",
            "balance",
            "birth_date",
            "phone",
            "address",
            "city",
            "province",
            "cap",
            "registration_date"
        ])

        # iterate for each user and create data for each user
        for i in range(1, n + 1):

            first_name = fake.first_name()      # create first name
            last_name = fake.last_name()        # create last name

            email = fake.email()                # create email
            while email in emails:              # check for dupicates
                email = fake.email()            # create new email
            emails.add(email)                   # unique new email

            # create random password
            password = generate_password()      # generate random password 
            password_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()

            birth_date = fake.date_of_birth(minimum_age=18, maximum_age=85) # generate the birth date
            phone_number = fake.phone_number()

            # create address (italian)
            address = fake.street_address()             # get random address (street and number)
            city = fake.city()                          # get random city
            cap = fake.postcode()                       # get random CAP
            province = random.choice(province_codes)    # get random province
            
            registration_date = fake.date_time_this_year()  # generate random date in the current year

            # write user's row in the .csv file 
            writer.writerow([
                i,                  # id of the user
                first_name,
                last_name,
                email,
                password_hash,
                "user",             # the user generated aren't administrator
                0,                  # initial account equal to 0
                birth_date,
                phone_number,
                address,
                city,
                province,
                cap,
                registration_date   
            ])

    print(f"Generated {n} users in the  '{output_file}' file.")

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    generate_users()        
