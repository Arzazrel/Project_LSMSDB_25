"""
Author: Alessandro Diana
Description: 
    Python programme to process the separate news datasets used in the project and create a single dataset with the processed data to be loaded into the Document database.
    The input CSV files are the CSV files for the datasets used for the project that need to be cleaned and integrated. 
    The input file names will be: Fin_Cleaned.csv,financial_news_events.csv, dataset.csv.
"""
import csv
import pandas as pd

FINAL_COLUMNS = ["Date", "Title", "Summary", "Text", "Sector", "Index", "Company"]  # name of the columns of the final news dataset

# ------------------------------------ start: utils methods ------------------------------------
"""
Automatically detects the delimiter of a CSV file.

Parameters:
- file_path: path to the CSV file
- n_lines: number of lines to read to detect the delimiter (default=20)

Returns:
- delimiter as a string (e.g., ',', ';', '\t', etc.)
"""
def get_csv_delimiter(file_path: str, n_lines: int = 20) -> str:
    with open(file_path, "r", encoding="utf-8") as f:
        sample_lines = "".join([f.readline() for _ in range(n_lines)])
        sniffer = csv.Sniffer()
        dialect = sniffer.sniff(sample_lines)
        delimiter = dialect.delimiter
    return delimiter

# function to normalize the dataset wih the name and columns of the final news dataset
def normalize_df(df: pd.DataFrame, mapping: dict) -> pd.DataFrame:

    df = df.rename(columns=mapping)     # rename column
    df = df[list(mapping.values())]     # take only the renamed columns

    # add missed column
    for col in FINAL_COLUMNS:
        if col not in df.columns:
            df[col] = ""

    return df[FINAL_COLUMNS]

# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: process news dataset methods ------------------------------------

# function to process the data news of the first dataset
def process_fin_cleaned(input_csv_path: str) -> pd.DataFrame:
    delimiter = get_csv_delimiter(input_csv_path)                           # get delimiter
    df = pd.read_csv(input_csv_path, sep = delimiter, dtype=str).fillna("") # read csv file
    
    # define the mapping to rename the columns
    mapping = {
        "Date_published": "Date",
        "Headline": "Title",
        "Synopsis": "Summary",
        "Full_text": "Text",
    }

    return normalize_df(df, mapping)

# function to process the data news of the second dataset
def process_financial_news_events(input_csv_path: str) -> pd.DataFrame:
    delimiter = get_csv_delimiter(input_csv_path)                           # get delimiter
    df = pd.read_csv(input_csv_path, sep = delimiter, dtype=str).fillna("") # read csv file
    
    # define the mapping to rename the columns
    mapping = {
        "Date": "Date",
        "Headline": "Title",
        "Market_Index": "Index",
        "Sector": "Sector",
        "Related_Company": "Company",
        "News_Url": "Text",
    }

    return normalize_df(df, mapping)

# function to process the data news of the thirth dataset
def process_dataset(input_csv_path: str) -> pd.DataFrame:
    delimiter = get_csv_delimiter(input_csv_path)                           # get delimiter
    df = pd.read_csv(input_csv_path, sep = delimiter, dtype=str).fillna("") # read csv file
    
    # define the mapping to rename the columns
    mapping = {
        "Date": "Date",
        "ParaphrasedSubject": "Title",
        "CompactedSummary": "Summary",
        "Content": "Text",
        "Subject": "Sector",
    }

    return normalize_df(df, mapping)
    
# ------------------------------------ end: process news dataset methods ------------------------------------

# function to call the preprocessing of the news dataset, to concat and create the final news dataset
def build_final_dataset(fin_cleaned_path: str,financial_events_path: str,dataset_path: str,output_path: str = "preprocessed_dataset.csv"):
    
    df1 = process_fin_cleaned(fin_cleaned_path)                 # process the first news dataset
    df2 = process_financial_news_events(financial_events_path)  # process the second news dataset
    df3 = process_dataset(dataset_path)                         # process the third news dataset

    final_df = pd.concat([df1, df2, df3], ignore_index=True)    # concat the processed dataset
    # clean 
    final_df = final_df.dropna(how="all")                       # remove empty rows
    final_df = final_df.apply(
        lambda col: col.str.strip() if col.dtype == "object" else col)  # Removes unnecessary spaces from string columns only
    
    # Normalize Sector: empty / NaN → "unknown"
    final_df["Sector"] = (
        final_df["Sector"]
        .fillna("unknown")            # NaN → unknown
        .astype(str)
        .str.strip()                  # remove spaces
    )
    final_df.loc[final_df["Sector"] == "", "Sector"] = "unknown"
    
    # convert in output .csv file
    final_df.to_csv(output_path,index=False,encoding="utf-8-sig",sep=",",quoting=csv.QUOTE_MINIMAL,lineterminator="\n",escapechar="\\") 

    print(f"Final dataset created: {output_path}")              # UI print
    print(f"Total rows: {len(final_df)}")                       # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    build_final_dataset(
        fin_cleaned_path="Fin_Cleaned.csv",
        financial_events_path="financial_news_events.csv",
        dataset_path="dataset.csv",
        output_path="final_news_ds.csv"
    )
